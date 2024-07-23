package app;

import java.io.Serializable;
import java.util.List;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public class ServentInfo implements Serializable {

    private static final long serialVersionUID = 5304170042791281555L;
    private final int id;
    private final String ipAddress;
    private final int listenerPort;
    private final List<Integer> neighbors;
    private int versionId;

    public ServentInfo(String ipAddress, int id, int listenerPort, List<Integer> neighbors, int versionId) {
        this.ipAddress = ipAddress;
        this.listenerPort = listenerPort;
        this.id = id;
        this.neighbors = neighbors;
        this.versionId = versionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getListenerPort() {
        return listenerPort;
    }

    public int getId() {
        return id;
    }

    public List<Integer> getNeighbors() {
        return neighbors;
    }

    public int getVersionId() {
        return versionId;
    }


    public void setVersionId(int versionId) {

       this.versionId = versionId;
    }

    @Override
    public String toString() {
        return "[" + id + "|" + ipAddress + "|" + listenerPort + "]" + " Version id " + versionId + " VOZDRA";
    }
}
