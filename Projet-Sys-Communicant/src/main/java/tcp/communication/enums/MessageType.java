package tcp.communication.enums;

public enum MessageType {
    USER_LIST,
    IMAGE,
    IMAGE_PRIVATE,
    BROADCAST,
    PRIVATE,
    UNKNOWN,
    FILE_PRIVATE,
    FILE;

    public static MessageType from(String message) {
        if (message.startsWith("USER_LIST:")) return USER_LIST;
        if (message.startsWith("IMAGE|")) return IMAGE;
        if (message.startsWith("IMAGE_PRIVATE|")) return IMAGE_PRIVATE;
        if (message.startsWith("BROADCAST|")) return BROADCAST;
        if (message.startsWith("PRIVATE|")) return PRIVATE;
        if (message.startsWith("FILE_PRIVATE|")) return FILE_PRIVATE;
        if (message.startsWith("FILE|")) return FILE;

        return UNKNOWN;
    }
}