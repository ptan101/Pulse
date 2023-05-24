package tan.philip.nrf_ble.SickbayPush;

public class SickbayImportSettings {
    private String namespace = "DEFAULT_NAMESPACE";

    //Settings imported from .init file.
    public SickbayImportSettings() {

    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
