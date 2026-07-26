package bg.edu.utp.aitravelbenchmark.ai;

public interface AiProviderClient {
    String getProviderName();

    String generate(String instruction);
}
