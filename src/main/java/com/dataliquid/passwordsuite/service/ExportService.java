package com.dataliquid.passwordsuite.service;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigFormat;
import com.dataliquid.passwordsuite.domain.ConfigGroup;
import com.dataliquid.passwordsuite.domain.ConfigNode;
import com.dataliquid.passwordsuite.domain.ConfigTree;

/**
 * Service for exporting ConfigTree to YAML or Properties format.
 */
public class ExportService {

    /**
     * Exports a ConfigTree to its original format (YAML or Properties).
     *
     * @param  tree the tree to export
     *
     * @return      the exported string
     */
    public String export(ConfigTree tree) {
        if (tree.getFormat() == ConfigFormat.YAML) {
            return exportToYaml(tree);
        } else if (tree.getFormat() == ConfigFormat.PROPERTIES) {
            return exportToProperties(tree);
        } else {
            // Default to YAML
            return exportToYaml(tree);
        }
    }

    /**
     * Exports a ConfigTree to YAML format.
     *
     * @param  tree the tree to export
     *
     * @return      the YAML string
     */
    public String exportToYaml(ConfigTree tree) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        Map<String, Object> data = treeToMap(tree.getRoot());

        return yaml.dump(data);
    }

    /**
     * Exports a ConfigTree to Properties format.
     *
     * @param  tree the tree to export
     *
     * @return      the Properties string
     */
    public String exportToProperties(ConfigTree tree) {
        Properties props = new Properties();
        flattenTree(tree.getRoot(), "", props);

        StringWriter writer = new StringWriter();
        try {
            props.store(writer, null);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export to Properties: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a ConfigGroup to a nested Map structure for YAML export. Note:
     * LinkedHashMap is intentionally used to preserve insertion order for YAML
     * output.
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<String, Object> treeToMap(ConfigGroup group) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (ConfigNode child : group.getChildren()) {
            if (child instanceof ConfigEntry) {
                ConfigEntry entry = (ConfigEntry) child;
                map.put(entry.getKey(), entry.getValue());
            } else if (child instanceof ConfigGroup) {
                ConfigGroup childGroup = (ConfigGroup) child;
                map.put(childGroup.getKey(), treeToMap(childGroup));
            }
        }

        return map;
    }

    /**
     * Flattens a ConfigTree into a Properties object with dot-notation keys.
     */
    private void flattenTree(ConfigNode node, String prefix, Properties props) {
        if (node instanceof ConfigEntry) {
            ConfigEntry entry = (ConfigEntry) node;
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            props.setProperty(key, entry.getValue());
        } else if (node instanceof ConfigGroup) {
            ConfigGroup group = (ConfigGroup) node;
            String newPrefix = prefix.isEmpty() ? group.getKey() : prefix + "." + group.getKey();

            // Skip "root" prefix
            if ("root".equals(group.getKey()) && prefix.isEmpty()) {
                newPrefix = "";
            }

            for (ConfigNode child : group.getChildren()) {
                flattenTree(child, newPrefix, props);
            }
        }
    }
}
