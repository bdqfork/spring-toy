package cn.bdqfork.core.container;

/**
 * @author bdq
 * @date 2019-02-13
 */
public abstract class AbstractInjectorData implements InjectorData {
    private String defalultName;
    private String refName;
    private BeanDefination bean;
    private boolean isRequired;
    private boolean isProvider;
    private Class<?> providedType;

    public AbstractInjectorData(String defalultName, String refName, boolean isRequired) {
        this.defalultName = defalultName;
        this.refName = refName;
        this.isRequired = isRequired;
    }

    @Override
    public void setDefaultName(String defaultName) {
        this.defalultName = defaultName;
    }

    @Override
    public String getDefaultName() {
        return defalultName;
    }

    @Override
    public String getRefName() {
        return refName;
    }

    @Override
    public void setBean(BeanDefination bean) {
        this.bean = bean;
    }

    @Override
    public BeanDefination getBean() {
        return this.bean;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public boolean isMatch(BeanDefination beanDefination) {
        if (refName != null && refName.equals(beanDefination.getName())) {
            return true;
        } else if (defalultName.equals(beanDefination.getName())) {
            return true;
        } else {
            Class<?> type = getType();
            return beanDefination.isType(type);
        }
    }

    @Override
    public void setProvider(boolean provider) {
        isProvider = provider;
    }

    @Override
    public boolean isProvider() {
        return isProvider;
    }

    @Override
    public void setProvidedType(Class<?> providedType) {
        this.providedType = providedType;
    }

    protected Class<?> getProvidedType() {
        return providedType;
    }
}