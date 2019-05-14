package pl.psypal.setivoappupdater;

public class SetivoConfig {

    private int version;
    private String file;

    public SetivoConfig() {}

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
