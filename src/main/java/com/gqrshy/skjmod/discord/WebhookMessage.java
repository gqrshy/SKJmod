package com.gqrshy.skjmod.discord;

import java.util.List;

public class WebhookMessage {
    private List<Embed> embeds;
    
    public WebhookMessage(List<Embed> embeds) {
        this.embeds = embeds;
    }
    
    public List<Embed> getEmbeds() {
        return embeds;
    }
    
    public void setEmbeds(List<Embed> embeds) {
        this.embeds = embeds;
    }
    
    public static class Embed {
        private String title;
        private Integer color;
        private List<Field> fields;
        
        public Embed(String title, Integer color, List<Field> fields) {
            this.title = title;
            this.color = color;
            this.fields = fields;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public Integer getColor() {
            return color;
        }
        
        public void setColor(Integer color) {
            this.color = color;
        }
        
        public List<Field> getFields() {
            return fields;
        }
        
        public void setFields(List<Field> fields) {
            this.fields = fields;
        }
    }
    
    public static class Field {
        private String name;
        private String value;
        private Boolean inline;
        
        public Field(String name, String value, Boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public Boolean getInline() {
            return inline;
        }
        
        public void setInline(Boolean inline) {
            this.inline = inline;
        }
    }
}