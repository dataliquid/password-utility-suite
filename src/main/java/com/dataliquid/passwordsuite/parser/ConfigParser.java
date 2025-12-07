package com.dataliquid.passwordsuite.parser;

import com.dataliquid.passwordsuite.domain.ConfigFormat;
import com.dataliquid.passwordsuite.domain.ConfigTree;

/**
 * Interface for parsing configuration content into a ConfigTree.
 */
public interface ConfigParser {

    /**
     * Parses the given content into a ConfigTree.
     *
     * @param  content        the configuration content to parse
     *
     * @return                the parsed ConfigTree
     *
     * @throws ParseException if the content cannot be parsed
     */
    ConfigTree parse(String content) throws ParseException;

    /**
     * Returns the format supported by this parser.
     */
    ConfigFormat getSupportedFormat();
}
