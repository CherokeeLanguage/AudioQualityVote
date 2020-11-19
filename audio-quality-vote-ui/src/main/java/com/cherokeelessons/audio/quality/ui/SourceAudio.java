package com.cherokeelessons.audio.quality.ui;


import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SourceElement;
import gwt.material.design.client.base.MaterialWidget;

public class SourceAudio extends MaterialWidget {

    public enum Type {
        MPEG,
        OGG,
        WAV
    }

    protected SourceElement element;
    protected Type type = Type.MPEG;
    protected String src;
    protected String alt;

    public SourceAudio() {
        super(Document.get().createSourceElement());
    }

    public SourceAudio(String src, Type type) {
        this();

        this.src = src;
        this.type = type;
    }

    @Override
    protected void onLoad() {
        super.onLoad();

        element = getElement().cast();
        element.setType("audio/" + type.name().toLowerCase());

        if (src != null && !src.isEmpty()) {
            element.setSrc(src);
        }

        if (alt != null && !alt.isEmpty()) {
            element.setAttribute("alt", alt);
        }
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }
}
