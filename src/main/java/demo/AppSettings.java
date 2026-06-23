package demo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppSettings {
    private final Environment environment;
    private final String nodeId;

    public AppSettings(Environment environment) {
        this.environment = environment;
        this.nodeId = read("TASK_ID", detectHostName());
    }

    public List<String> cacheNames() {
        return Arrays.stream(read("CACHE_NAMES", "CacheA,CacheB,CacheC").split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    public int owners() {
        return Integer.parseInt(read("CACHE_OWNERS", "2"));
    }

    public String clusterName() {
        return read("INFINISPAN_CLUSTER_NAME", "aws-infinispan-demo");
    }

    public String nodeId() {
        return nodeId;
    }

    public String stackFile() {
        String discovery = read("INFINISPAN_DISCOVERY", "local").toLowerCase(Locale.ROOT);
        if ("s3".equals(discovery)) {
            return read("INFINISPAN_JGROUPS_STACK", "jgroups-s3.xml");
        }
        return read("INFINISPAN_JGROUPS_STACK", "jgroups-local.xml");
    }

    public String s3PingBucket() {
        return read("S3_PING_BUCKET", "");
    }

    public String s3PingPrefix() {
        return read("S3_PING_PREFIX", "infinispan-demo");
    }

    public String awsRegion() {
        return read("AWS_REGION", "ap-northeast-1");
    }

    public String bindAddress() {
        return read("JGROUPS_BIND_ADDRESS", "0.0.0.0");
    }

    public String bindPort() {
        return read("JGROUPS_BIND_PORT", "7800");
    }

    public String tcppingInitialHosts() {
        return read("JGROUPS_TCPPING_INITIAL_HOSTS", "127.0.0.1[7800]");
    }

    private String read(String name, String defaultValue) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String detectHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-node";
        }
    }
}
