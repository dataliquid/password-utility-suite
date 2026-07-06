package com.dataliquid.passwordsuite.cli;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.AlgorithmConfig;
import com.dataliquid.passwordsuite.crypto.config.FormatConfig;
import com.dataliquid.passwordsuite.crypto.factory.CipherRegistry;
import com.dataliquid.passwordsuite.service.CryptoService;
import com.dataliquid.passwordsuite.service.CryptoServiceFactory;

/**
 * Command-line interface for encrypting and decrypting single values.
 * <p>
 * Usage:
 *
 * <pre>
 *   encrypt -p password -v value [-a algorithm] [-f format]
 *   decrypt -p password -v value [-a algorithm]
 *   algorithms
 *   help
 * </pre>
 */
@SuppressWarnings({ "PMD.SystemPrintln", "PMD.UseVarargs", "PMD.AvoidReassigningLoopVariables",
        "PMD.AvoidInstantiatingObjectsInLoops", "PMD.ConsecutiveAppendsShouldReuse", "PMD.AssignmentInOperand" })
public final class PasswordUtilitySuiteCLI {

    private static final String DEFAULT_ALGORITHM = "AES-256-GCM";
    private static final String DEFAULT_FORMAT_PREFIX = FormatConfig.DEFAULT_PREFIX;
    private static final String DEFAULT_FORMAT_SUFFIX = FormatConfig.DEFAULT_SUFFIX;

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_ERROR = 1;
    private static final int EXIT_INVALID_ARGS = 2;

    private final CipherRegistry cipherRegistry;
    private final CryptoServiceFactory cryptoServiceFactory;

    public PasswordUtilitySuiteCLI() {
        this.cipherRegistry = new CipherRegistry();
        this.cryptoServiceFactory = new CryptoServiceFactory(cipherRegistry);
    }

    /**
     * Options shared by the encrypt and decrypt commands.
     */
    private static final class CliOptions {
        private String password;
        private String value;
        private String algorithm = DEFAULT_ALGORITHM;
        private String formatPrefix;
        private boolean helpRequested;
    }

    public static void main(String[] args) {
        PasswordUtilitySuiteCLI cli = new PasswordUtilitySuiteCLI();
        int exitCode = cli.run(args);
        System.exit(exitCode);
    }

    /**
     * Runs the CLI with the given arguments.
     *
     * @param  args command-line arguments
     *
     * @return      exit code (0=success, 1=error, 2=invalid args)
     */
    public int run(String[] args) {
        if (args.length == 0) {
            printUsage();
            return EXIT_INVALID_ARGS;
        }

        String command = args[0].toLowerCase(Locale.ROOT);
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (command) {
        case "encrypt":
            return handleEncrypt(commandArgs);
        case "decrypt":
            return handleDecrypt(commandArgs);
        case "algorithms":
            return handleAlgorithms();
        case "help":
        case "--help":
        case "-h":
            printUsage();
            return EXIT_SUCCESS;
        default:
            System.err.println("Unknown command: " + command);
            printUsage();
            return EXIT_INVALID_ARGS;
        }
    }

    private int handleEncrypt(String[] args) {
        CliOptions options = parseOptions(args);
        if (options.helpRequested) {
            printEncryptUsage();
            return EXIT_SUCCESS;
        }

        Integer argError = resolveRequiredOptions(options, this::printEncryptUsage);
        if (argError != null) {
            return argError;
        }

        // Determine format prefix/suffix from algorithm if not specified
        String effectivePrefix = options.formatPrefix;
        String effectiveSuffix = DEFAULT_FORMAT_SUFFIX;
        if (effectivePrefix == null && cipherRegistry.hasAlgorithm(options.algorithm)) {
            AlgorithmConfig config = cipherRegistry.getAlgorithm(options.algorithm);
            effectivePrefix = config.getDefaultFormatPrefix();
            effectiveSuffix = config.getDefaultFormatSuffix();
        } else if (effectivePrefix == null) {
            effectivePrefix = DEFAULT_FORMAT_PREFIX;
        }

        return runCryptoOperation(options, effectivePrefix, effectiveSuffix, true);
    }

    private int handleDecrypt(String[] args) {
        CliOptions options = parseOptions(args);
        if (options.helpRequested) {
            printDecryptUsage();
            return EXIT_SUCCESS;
        }

        Integer argError = resolveRequiredOptions(options, this::printDecryptUsage);
        if (argError != null) {
            return argError;
        }

        // Determine format from algorithm
        String formatPrefix = DEFAULT_FORMAT_PREFIX;
        String formatSuffix = DEFAULT_FORMAT_SUFFIX;
        if (cipherRegistry.hasAlgorithm(options.algorithm)) {
            AlgorithmConfig config = cipherRegistry.getAlgorithm(options.algorithm);
            formatPrefix = config.getDefaultFormatPrefix();
            formatSuffix = config.getDefaultFormatSuffix();
        }

        return runCryptoOperation(options, formatPrefix, formatSuffix, false);
    }

    /**
     * Parses the common command-line options for encrypt and decrypt.
     */
    private CliOptions parseOptions(String[] args) {
        CliOptions options = new CliOptions();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (("--password".equals(arg) || "-p".equals(arg)) && i + 1 < args.length) {
                options.password = args[++i];
            } else if (("--value".equals(arg) || "-v".equals(arg)) && i + 1 < args.length) {
                options.value = args[++i];
            } else if (("--algorithm".equals(arg) || "-a".equals(arg)) && i + 1 < args.length) {
                options.algorithm = args[++i];
            } else if (("--format".equals(arg) || "-f".equals(arg)) && i + 1 < args.length) {
                options.formatPrefix = args[++i];
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                options.helpRequested = true;
            }
        }
        return options;
    }

    /**
     * Validates the required value option and prompts for the password if it was
     * not provided.
     *
     * @return an exit code on error, or null if all required options are present
     */
    private Integer resolveRequiredOptions(CliOptions options, Runnable printUsage) {
        if (options.value == null) {
            System.err.println("Error: --value is required");
            printUsage.run();
            return EXIT_INVALID_ARGS;
        }

        if (options.password == null) {
            options.password = promptPassword();
            if (options.password == null) {
                System.err.println("Error: Password is required");
                return EXIT_INVALID_ARGS;
            }
        }

        return null;
    }

    /**
     * Runs the encryption or decryption and prints the result.
     */
    private int runCryptoOperation(CliOptions options, String formatPrefix, String formatSuffix, boolean encrypt) {
        try {
            CryptoService cryptoService = cryptoServiceFactory
                    .create(options.password.toCharArray(), options.algorithm, formatPrefix, formatSuffix);
            System.out.println(encrypt ? cryptoService.encrypt(options.value) : cryptoService.decrypt(options.value));
            return EXIT_SUCCESS;
        } catch (CryptoException e) {
            System.err.println((encrypt ? "Encryption" : "Decryption") + " failed: " + e.getMessage());
            return EXIT_ERROR;
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            return EXIT_ERROR;
        }
    }

    private int handleAlgorithms() {
        System.out.println("Available algorithms:");
        List<String> algorithms = cipherRegistry.getAvailableAlgorithms();
        for (String algo : algorithms) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("  ");
            sb.append(algo);
            if (DEFAULT_ALGORITHM.equals(algo)) {
                sb.append(" (default)");
            }
            if (cipherRegistry.hasAlgorithm(algo)) {
                AlgorithmConfig config = cipherRegistry.getAlgorithm(algo);
                if (config.hasPasswordConstraints()) {
                    sb.append(" (requires ");
                    sb.append(config.getMinPasswordLength());
                    sb.append(" or ");
                    sb.append(config.getMaxPasswordLength());
                    sb.append(" char password)");
                }
            }
            System.out.println(sb);
        }
        return EXIT_SUCCESS;
    }

    private String promptPassword() {
        Console console = System.console();
        if (console == null) {
            System.err.println("Error: No console available for password input");
            return null;
        }

        char[] passwordChars = console.readPassword("Password: ");
        if (passwordChars == null || passwordChars.length == 0) {
            return null;
        }

        String password = new String(passwordChars);
        Arrays.fill(passwordChars, ' '); // Clear sensitive data
        return password;
    }

    private void printUsage() {
        System.out.println("Password Utility Suite CLI");
        System.out.println();
        System.out.println("Usage: java -cp <jar> " + getClass().getName() + " <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  encrypt     Encrypt a value");
        System.out.println("  decrypt     Decrypt a value");
        System.out.println("  algorithms  List available algorithms");
        System.out.println("  help        Show this help message");
        System.out.println();
        System.out.println("Use '<command> --help' for more information about a command.");
    }

    private void printEncryptUsage() {
        System.out.println("Usage: encrypt [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -p, --password <password>  Master password (prompted if not provided)");
        System.out.println("  -v, --value <value>        Value to encrypt (required)");
        System.out.println("  -a, --algorithm <algo>     Encryption algorithm (default: " + DEFAULT_ALGORITHM + ")");
        System.out.println("  -f, --format <prefix>      Format prefix (default: algorithm-specific)");
        System.out.println("  -h, --help                 Show this help message");
    }

    private void printDecryptUsage() {
        System.out.println("Usage: decrypt [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -p, --password <password>  Master password (prompted if not provided)");
        System.out.println("  -v, --value <value>        Encrypted value to decrypt (required)");
        System.out.println("  -a, --algorithm <algo>     Encryption algorithm (default: " + DEFAULT_ALGORITHM + ")");
        System.out.println("  -h, --help                 Show this help message");
    }
}
