package com.obdhondascan.model;

/**
 * Created by AlexFedorenko on 05.12.2017.
 */

//error model
public class OBDError {

    private int errorId;

    private boolean isError;

    public int getErrorId() {
        return errorId;
    }

    public void setErrorId(int errorId) {
        this.errorId = errorId;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }
}
