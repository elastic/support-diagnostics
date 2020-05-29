package com.elastic.support.scrub;

import com.elastic.support.Constants;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class ScrubProcessor {

    private static final Logger logger = LogManager.getLogger(ScrubProcessor.class);

    private static Map<String, Object> scrubConfig;

    private static ConcurrentHashMap<Integer, Integer> ipv4 = new ConcurrentHashMap<>();
    private static Vector<String> autoScrub = new Vector<>();
    private static Vector<String> globalExclude = new Vector<>();
    private static Vector<String> remove = new Vector<>();
    private static Vector<ScrubTokenEntry> tokens = new Vector<>();
    private static ConcurrentHashMap<String, String> clusterInfoCache = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();

    public ScrubProcessor(String nodes) {

        this();

        if (StringUtils.isNotEmpty(nodes)) {
            initAutoScrub(nodes);
        }
    }

    public ScrubProcessor() {
        scrubConfig =
                JsonYamlUtils.readYamlFromClasspath("scrub.yml", false);
        Collection auto = (Collection) scrubConfig.get("auto-scrub");
        if (auto != null) {
            autoScrub.addAll(auto);
        } else {
            logger.info(Constants.CONSOLE, "All autoscrub tokens disabled. Bypassing autoscrub processing");
        }

        Collection<String> removeTokens = (Collection) scrubConfig.get("remove");
        if (removeTokens != null) {
            remove.addAll(removeTokens);
        }

        Collection<String> exclude = (Collection) scrubConfig.get("global-exclude");
        if (exclude != null) {
            globalExclude.addAll(exclude);
        }

        initIpv4();
        initScrubTokens();
    }

    private void initAutoScrub(String nodes) {
        JsonNode nodesInfo = JsonYamlUtils.createJsonNodeFromString(nodes);
        if (autoScrub.contains("clusterName")) {
            String clusterName = nodesInfo.path("cluster_name").asText();
            clusterInfoCache.put(clusterName, generateToken(clusterName));
        }

        if (autoScrub.contains("nodeId") || autoScrub.contains("nodeName")) {
            JsonNode nodeEntries = nodesInfo.path("nodes");
            Iterator<Map.Entry<String, JsonNode>> iterNode = nodeEntries.fields();
            while (iterNode.hasNext()) {
                Map.Entry<String, JsonNode> n = iterNode.next();
                if (autoScrub.contains("nodeId")) {
                    clusterInfoCache.put(n.getKey(), generateToken(n.getKey()));
                }
                if (autoScrub.contains("nodeName")) {
                    JsonNode node = n.getValue();
                    String nodeName = node.path("name").asText();
                    clusterInfoCache.put(nodeName, generateToken(nodeName));
                }
            }
        }
    }

    private void initScrubTokens() {

        List<Map<String, Object>> configTokens = (List<Map<String, Object>>) scrubConfig.get("tokens");

        if (configTokens == null) {
            return;
        }

        for (Map<String, Object> t : configTokens) {
            String tkn = (String) t.get("token");
            List<String> inc = (List<String>) ObjectUtils.defaultIfNull(t.get("include"), new ArrayList<String>());
            List<String> exc = (List<String>) ObjectUtils.defaultIfNull(t.get("exclude"), new ArrayList<String>());
            tokens.add(new ScrubTokenEntry(tkn, inc, exc));
        }
        if (tokens.isEmpty()) {
            if (tokens.size() == 0) {
                logger.info(Constants.CONSOLE, "Scrubbing was enabled but no tokens were defined. Bypassing custom token processing.");
            }
        }

    }

    private void initIpv4() {

        Random random = new Random();
        IntStream intStream = random.ints(300, 556).distinct().limit(256);

        int key = 0;
        int[] vals = intStream.toArray();
        for (int i = 0; i < 256; i++) {
            ipv4.put(i, vals[i]);
        }

    }

    public boolean isMatch(Vector<String> regexs, String entry) {
        for (String regx : regexs) {
            if (entry.matches(regx)) {
                return true;
            }
        }
        return false;

    }

    public boolean isRemove(String entry) {
        return isMatch(remove, entry);
    }

    public boolean isExclude(String entry) {
        return isMatch(globalExclude, entry);
    }

    public String scrubIPv4(String input) {

        StringBuffer newIp = new StringBuffer();
        String[] ipSegments = input.split("\\.");
        for (int i = 0; i < 4; i++) {
            int set = Integer.parseInt(ipSegments[i]);
            if (!ipv4.containsKey(set)) {
                logger.info("Error converting ip segment {} from address: {}", Integer.toString(set));
                throw new RuntimeException("Error scrubbing IP Addresses");
            }
            int replace = ipv4.get(set);
            newIp.append(replace);
            if (i < 3) {
                newIp.append(".");
            }
        }
        return newIp.toString();
    }

    public String scrubIPv6(String input) {

        String[] ipSegments = input.split(":");
        int sz = ipSegments.length;
        StringBuilder newIp = new StringBuilder();

        for (int i = 0; i < sz; i++) {
            newIp.append(generateToken(ipSegments[i]));
            if (i < (sz - 1)) {
                newIp.append(":");
            }
        }
        return newIp.toString();
    }

    public String generateToken(String token) {

        if (StringUtils.isEmpty(token)) {
            return "";
        }

        StringBuilder newToken = new StringBuilder();
        int len = token.length();
        int passes = 1;
        if (len > 32) {
            passes = (len / 32) + 1;
        }

        for (int i = 0; i < passes; i++) {
            newToken.append(
                    UUID.nameUUIDFromBytes(token.getBytes()).toString()
                            .replaceAll("-", "")
            );
        }

        return newToken.toString().substring(0, len);

    }

    public String processLineWithTokens(String line, String entry) {
        for (ScrubTokenEntry token : tokens) {
            if (!token.include.isEmpty()) {
                boolean filtered = true;
                for (String inc : token.include) {
                    if (entry.matches(inc)) {
                        logger.info(Constants.CONSOLE, "Include rule applied for: {}.", entry);
                        filtered = false;
                        break;
                    }
                }
                if (filtered) {
                    continue;
                }
            }

            if (!token.exclude.isEmpty()) {
                boolean filtered = false;
                for (String exc : token.exclude) {
                    if (entry.matches(exc)) {
                        logger.info(Constants.CONSOLE, "Exclude rule applied for: {}.", entry);
                        filtered = true;
                        break;
                    }
                }
                if (filtered) {
                    continue;
                }
            }

            Matcher matcher = token.pattern.matcher(line);
            if (matcher.find()) {
                String group = matcher.group();
                String replacement = tokenCache.computeIfAbsent(group, k -> generateToken(k));
                logger.trace("{} generated {}", group, replacement);
                line = matcher.replaceAll(replacement);
            }

        }
        return line;
    }

    public String processMacddresses(String input) {

        String content = input;
        Pattern pattern = Pattern.compile(Constants.MacAddrRegex);
        Matcher matcher = pattern.matcher(content);
        if(matcher.find()){
            String group = matcher.group();
            content = content.replaceAll(group, "XX:XX:XX:XX:XX:XX");
        }
        return content;

    }

    private String processIpv4Addresses(String input) {

        String content = input;
        Pattern pattern = Pattern.compile(Constants.IPv4Regex);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String group = matcher.group();
            String replacement = tokenCache.computeIfAbsent(group, k -> scrubIPv4(k));
            content = content.replaceAll(group, replacement);
        }

        return content;

    }


    private String processIpv6Addresses(String input) {

        String content = input;
        Pattern pattern = Pattern.compile(Constants.IPv6Regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String group = matcher.group();
            String replacement = tokenCache.computeIfAbsent(group, k -> scrubIPv6(k));
            content = content.replaceAll(group, replacement);
        }

        return content;
    }

    private String processClusterArtifacts(String input) {
        String content = input;
        for (Map.Entry<String, String> entry : clusterInfoCache.entrySet()) {
            content = content.replaceAll(entry.getKey(), entry.getValue());
        }

        return content;

    }

    public String processAutoscrub(String input) {
        String content = input;
        if (autoScrub.contains("ipv4")) {
            content = processIpv4Addresses(content);
        }
        if (autoScrub.contains("ipv6")) {
            content = processIpv6Addresses(content);
        }
        if (autoScrub.contains("mac")) {
            content = processMacddresses(content);
        }
        content = processClusterArtifacts(content);

        return content;

    }

}



