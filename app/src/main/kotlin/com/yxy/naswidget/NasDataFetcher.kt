package com.yxy.naswidget

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/** 容器生命周期操作类型 */
enum class ContainerAction { START, STOP, RESTART }

/** 从 monitor_proxy / 飞牛 API 拉取 NAS 数据 */
object NasDataFetcher {

    private var dockerCache: List<DockerContainer>? = null
    private var dockerCacheTime = 0L
    private const val DOCKER_CACHE_MS = 30000L

    fun fetch(url: String): Result<NasStats> {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return Result.failure(Exception("HTTP $code"))
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            Result.success(NasStats.parse(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** POST /ogh/ac/w 通用 appcgi 请求，返回 (httpCode, body) */
    private fun postAppcgi(nasBase: String, accessToken: String, req: String, extra: (JSONObject) -> Unit): Pair<Int, String> {
        val conn = URL(nasBase.trimEnd('/') + "/ogh/ac/w").openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.doOutput = true
        val body = JSONObject().apply {
            put("req", req)
            put("reqid", java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16))
            extra(this)
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = conn.responseCode
        val resp = conn.inputStream?.bufferedReader()?.use { it.readText() }
            ?: conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        return code to resp
    }

    /** 解析 appcgi 响应：数组取 [0]，否则当对象 */
    private fun parseRootObj(resp: String): JSONObject? {
        val rootArr = runCatching { JSONArray(resp) }.getOrNull()
        return rootArr?.optJSONObject(0) ?: runCatching { JSONObject(resp) }.getOrNull()
    }

    /**
     * 从飞牛 appcgi 拉取 Docker 容器列表（含运行中+停止 + 各 running 容器内存占用）。
     * 飞牛 OAuth access_token 走 /ogh/ac/w + Authorization: Bearer，无需 HMAC 签名。
     */
    fun fetchDocker(nasBase: String, accessToken: String): Result<List<DockerContainer>> {
        val now = System.currentTimeMillis()
        if (dockerCache != null && now - dockerCacheTime < DOCKER_CACHE_MS) {
            return Result.success(dockerCache!!)
        }
        return try {
            val (code, resp) = postAppcgi(nasBase, accessToken, "appcgi.dockermgr.containerList") { it.put("all", true) }
            if (code != 200) return Result.failure(Exception("HTTP $code: $resp"))

            val rootObj = parseRootObj(resp)
            val arr = rootObj?.optJSONArray("rsp")
                ?: rootObj?.optJSONArray("data")
                ?: rootObj?.optJSONObject("data")?.optJSONArray("containers")
            val list = mutableListOf<DockerContainer>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    val names = c.optJSONArray("Names")
                    val rawName = if (names != null && names.length() > 0)
                        names.optString(0) else c.optString("Name", c.optString("name", ""))
                    val name = rawName.removePrefix("/")
                    val state = c.optString("State", c.optString("state", ""))
                    val status = c.optString("Status", c.optString("status", ""))
                    val id = c.optString("Id", c.optString("id", ""))
                    if (name.isNotEmpty()) {
                        list.add(DockerContainer(id, name, state, status, null))
                    }
                }
            }
            val result = rootObj?.optString("result", "")
            if (list.isEmpty() && !result.equals("succ", true)) {
                return Result.failure(Exception("响应异常: $resp"))
            }
            Result.success(fetchMemUsage(nasBase, accessToken, list)).also { r ->
                r.onSuccess { l -> dockerCache = l; dockerCacheTime = now }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 并发拉取各 running 容器的内存占用（containerStats.usedMem） */
    private fun fetchMemUsage(nasBase: String, accessToken: String, containers: List<DockerContainer>): List<DockerContainer> {
        val running = containers.filter { it.running && it.id.isNotEmpty() }
        if (running.isEmpty()) return containers
        val pool = java.util.concurrent.Executors.newFixedThreadPool(Math.min(8, running.size))
        try {
            val futures = running.map { c ->
                pool.submit(java.util.concurrent.Callable { c.id to fetchContainerStats(nasBase, accessToken, c.id) })
            }
            val memMap = futures.map { it.get() }.toMap()
            return containers.map { c -> if (c.running) c.copy(memBytes = memMap[c.id]) else c }
        } finally {
            pool.shutdown()
        }
    }

    /** 单容器 stats，返回 usedMem 字节；失败返回 null */
    private fun fetchContainerStats(nasBase: String, accessToken: String, containerId: String): Long? {
        return try {
            val (code, resp) = postAppcgi(nasBase, accessToken, "appcgi.dockermgr.containerStats") {
                it.put("containerId", containerId)
            }
            if (code != 200) return null
            val rootObj = parseRootObj(resp)
            if (rootObj?.optString("result", "") != "succ") return null
            rootObj.optJSONObject("rsp")?.optLong("usedMem")
        } catch (e: Exception) {
            null
        }
    }

    /** 容器生命周期操作：启动 / 停止 / 重启（appcgi.dockermgr.containerStart/Stop/Restart，containerId 标识） */
    fun containerAction(nasBase: String, accessToken: String, containerId: String, action: ContainerAction): Result<Unit> {
        return try {
            val req = when (action) {
                ContainerAction.START -> "appcgi.dockermgr.containerStart"
                ContainerAction.STOP -> "appcgi.dockermgr.containerStop"
                ContainerAction.RESTART -> "appcgi.dockermgr.containerRestart"
            }
            val (code, resp) = postAppcgi(nasBase, accessToken, req) { it.put("containerId", containerId) }
            if (code != 200) return Result.failure(Exception("HTTP $code: $resp"))
            val rootObj = parseRootObj(resp)
            val result = rootObj?.optString("result", "") ?: ""
            val errmsg = rootObj?.optString("errmsg", "")
            if (!result.equals("succ", true)) {
                return Result.failure(Exception(errmsg.ifEmpty { "操作失败" }))
            }
            dockerCache = null  // 清缓存，下次列表拉取最新状态
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 拉取 wb2api（workbuddy2api）积分统计：GET /panel/api/overview，Bearer 认证 */
    fun fetchWb2apiCredits(base: String, apiKey: String): Result<Wb2apiStats> {
        return try {
            val conn = URL(base.trimEnd('/') + "/panel/api/overview").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            val code = conn.responseCode
            val body = conn.inputStream?.bufferedReader()?.use { it.readText() }
                ?: conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code != 200) return Result.failure(Exception("HTTP $code: $body"))

            val root = JSONObject(body)
            val arr = root.optJSONArray("accounts") ?: return Result.failure(Exception("响应无 accounts"))
            val accounts = mutableListOf<Wb2apiStats.AccountCredit>()
            var total = 0L
            var healthy = 0
            for (i in 0 until arr.length()) {
                val a = arr.optJSONObject(i) ?: continue
                val credits = a.optLong("credits", 0L)
                val cooling = a.optBoolean("cooling", false)
                val disabled = a.optBoolean("disabled", false)
                total += credits
                if (!cooling && !disabled) healthy++
                accounts.add(
                    Wb2apiStats.AccountCredit(
                        a.optString("nickname", "?"),
                        credits,
                        a.optLong("credits_total", 0L),
                        cooling,
                        disabled
                    )
                )
            }
            Result.success(Wb2apiStats(total, accounts.size, healthy, accounts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
