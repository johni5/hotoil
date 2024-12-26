package com.del.hotoil;

public class Error {

    private int messageId;
    private String msg;
    private Exception exception;


    public Error(int messageId) {
        this.messageId = messageId;
    }

    public Error(int messageId, String msg, Exception exception) {
        this.messageId = messageId;
        this.msg = msg;
        this.exception = exception;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getMsg() {
        return msg;
    }

    public Exception getException() {
        return exception;
    }
}
