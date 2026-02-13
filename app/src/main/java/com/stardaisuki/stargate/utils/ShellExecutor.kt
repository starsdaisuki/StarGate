package com.stardaisuki.stargate.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Root Shell 命令执行器
 */
object ShellExecutor {

    private const val TAG = "StarGate.Shell"

    data class ShellResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val isSuccess get() = exitCode == 0
    }

    /**
     * 执行普通 shell 命令
     */
    suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "exec: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            Log.d(TAG, "exec result: code=$exitCode, stdout=$stdout, stderr=$stderr")
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "exec error: ${e.message}")
            ShellResult(-1, "", e.message ?: "Unknown error")
        }
    }

    /**
     * 执行 root 命令
     */
    suspend fun execRoot(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "execRoot: $command")
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            Log.d(TAG, "execRoot result: code=$exitCode, stdout=$stdout, stderr=$stderr")
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "execRoot error: ${e.message}")
            ShellResult(-1, "", e.message ?: "Unknown error")
        }
    }

    /**
     * 执行多条 root 命令（逐条执行，不因某条失败而中断）
     */
    suspend fun execRootCommands(commands: List<String>): ShellResult = withContext(Dispatchers.IO) {
        try {
            // 用换行符拼接所有命令，通过 stdin 传给 su
            val process = Runtime.getRuntime().exec("su")
            val outputStream = process.outputStream.bufferedWriter()

            for (cmd in commands) {
                Log.d(TAG, "execRoot cmd: $cmd")
                outputStream.write(cmd)
                outputStream.newLine()
            }
            outputStream.write("exit")
            outputStream.newLine()
            outputStream.flush()
            outputStream.close()

            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            Log.d(TAG, "execRootCommands result: code=$exitCode, stdout=$stdout, stderr=$stderr")
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "execRootCommands error: ${e.message}")
            ShellResult(-1, "", e.message ?: "Unknown error")
        }
    }

    /**
     * 检查是否有 root 权限
     */
    suspend fun checkRoot(): Boolean {
        return try {
            val result = execRoot("id")
            val hasRoot = result.isSuccess && result.stdout.contains("uid=0")
            Log.d(TAG, "checkRoot: $hasRoot (${result.stdout})")
            hasRoot
        } catch (e: Exception) {
            Log.e(TAG, "checkRoot error: ${e.message}")
            false
        }
    }
}
