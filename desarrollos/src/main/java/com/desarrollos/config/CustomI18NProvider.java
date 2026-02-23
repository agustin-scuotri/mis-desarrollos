package com.desarrollos.config;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CustomI18NProvider implements I18NProvider {
    @Override
    public List<Locale> getProvidedLocales() {
        return Collections.singletonList(new Locale("es"));
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        return bundle.getString(key);
    }
}