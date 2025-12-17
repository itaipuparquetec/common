package br.org.itaipuparquetec.common.infrastructure.i18n;

import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * @author Emanuel Victor
 * @version 1.0.0
 * @since 1.0.0, 10/09/2019
 */
public class ResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {

    /**
     *
     */
    public ResourceBundleMessageSource() {
        MessageSourceHolder.setMessageSource(this);
    }

}
