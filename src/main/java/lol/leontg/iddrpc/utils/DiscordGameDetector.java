package lol.leontg.iddrpc.utils;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;

import lol.leontg.iddrpc.Main;

/**
 * @author Whoever made this, I love you omg
 */
public class DiscordGameDetector {
    private static final int PROCESS_INFORMATION_CLASS_BASIC = 0;
    private static final int PROCESS_BASIC_INFORMATION_SIZE_X64 = 0x30;

    private static final int PEB_BASE_ADDRESS_OFFSET_X64 = 0x08;
    private static final int PEB_PROCESS_PARAMETERS_OFFSET_X64 = 0x20;

    private static final int RTL_USER_PROCESS_PARAMETERS_CMD_LINE_LENGTH_OFFSET_X64 = 0x70;
    private static final int RTL_USER_PROCESS_PARAMETERS_CMD_LINE_BUFFER_OFFSET_X64 = 0x78;

    private interface NtDll extends com.sun.jna.Library {
        NtDll INSTANCE = Native.load("ntdll", NtDll.class, W32APIOptions.DEFAULT_OPTIONS);

        int NtQueryInformationProcess(
                HANDLE processHandle,
                int processInformationClass,
                Pointer processInformation,
                int processInformationLength,
                int[] returnLength
        );
    }

    public static void makeDiscordNotDetectDefaultMinecraftRPC() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        HANDLE hProcess = Kernel32.INSTANCE.GetCurrentProcess();
        Memory pbi = new Memory(PROCESS_BASIC_INFORMATION_SIZE_X64);

        int status = NtDll.INSTANCE.NtQueryInformationProcess(hProcess, PROCESS_INFORMATION_CLASS_BASIC, pbi, (int) pbi.size(), new int[1]);

        if (status != 0) {
            return;
        }

        Pointer pebAddress = pbi.getPointer(PEB_BASE_ADDRESS_OFFSET_X64);

        if (pebAddress == null) {
            return;
        }

        Pointer processParameters = pebAddress.getPointer(PEB_PROCESS_PARAMETERS_OFFSET_X64);

        if (processParameters == null) {
            return;
        }

        short cmdLengthBytes = processParameters.getShort(RTL_USER_PROCESS_PARAMETERS_CMD_LINE_LENGTH_OFFSET_X64);
        Pointer cmdBuffer = processParameters.getPointer(RTL_USER_PROCESS_PARAMETERS_CMD_LINE_BUFFER_OFFSET_X64);

        if (cmdBuffer == null || cmdLengthBytes <= 0) {
            return;
        }

        int charCount = cmdLengthBytes / 2;
        char[] chars = cmdBuffer.getCharArray(0, charCount);

        String originalCmd = new String(chars);

        // Discord does not detect Minecraft as a game if it can't find it in the list of processes, these are the words it (allegedly) looks for
        String scrubbed = originalCmd
                .replace("net.minecraft", "             ")
                .replace("KnotClient", "          ")
                .replace("theseus", "       ");

        char[] scrubbedChars = scrubbed.toCharArray();
        cmdBuffer.write(0, scrubbedChars, 0, Math.min(chars.length, scrubbedChars.length));

        Main.LOGGER.info("[DiscordRPC] Successfully disabled Discord displaying the default Minecraft Rich Presence");
    }
}
