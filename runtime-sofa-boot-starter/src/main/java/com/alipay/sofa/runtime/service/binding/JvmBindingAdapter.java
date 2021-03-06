/**
 * Copyright Notice: This software is developed by Ant Small and Micro Financial Services Group Co., Ltd. This software and all the relevant information, including but not limited to any signs, images, photographs, animations, text, interface design,
 *  audios and videos, and printed materials, are protected by copyright laws and other intellectual property laws and treaties.
 *  The use of this software shall abide by the laws and regulations as well as Software Installation License Agreement/Software Use Agreement updated from time to time.
 *   Without authorization from Ant Small and Micro Financial Services Group Co., Ltd., no one may conduct the following actions:
 *
 *   1) reproduce, spread, present, set up a mirror of, upload, download this software;
 *
 *   2) reverse engineer, decompile the source code of this software or try to find the source code in any other ways;
 *
 *   3) modify, translate and adapt this software, or develop derivative products, works, and services based on this software;
 *
 *   4) distribute, lease, rent, sub-license, demise or transfer any rights in relation to this software, or authorize the reproduction of this software on other’s computers.
 */
package com.alipay.sofa.runtime.service.binding;

import com.alipay.sofa.runtime.api.binding.BindingType;
import com.alipay.sofa.runtime.api.component.ComponentName;
import com.alipay.sofa.runtime.service.component.ServiceComponent;
import com.alipay.sofa.runtime.spi.binding.BindingAdapter;
import com.alipay.sofa.runtime.spi.binding.Contract;
import com.alipay.sofa.runtime.spi.component.ComponentInfo;
import com.alipay.sofa.runtime.spi.service.ServiceProxy;
import com.alipay.sofa.runtime.spi.component.SofaRuntimeContext;
import com.alipay.sofa.runtime.spi.log.SofaLogger;
import com.alipay.sofa.runtime.spi.util.ComponentNameFactory;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/**
 * JVM Binding Adapter, used to handle JvmBinding
 *
 * @author xuanbei 18/2/28
 */
public class JvmBindingAdapter implements BindingAdapter<JvmBinding> {

    public JvmBindingAdapter() {
    }

    @Override
    public void preOutBinding(Object contract, JvmBinding binding, Object target,
                              SofaRuntimeContext sofaRuntimeContext) {
    }

    @Override
    public Object outBinding(Object contract, JvmBinding binding, Object target,
                             SofaRuntimeContext sofaRuntimeContext) {
        return null;
    }

    @Override
    public void preUnoutBinding(Object contract, JvmBinding binding, Object target,
                                SofaRuntimeContext sofaRuntimeContext) {

    }

    @Override
    public void postUnoutBinding(Object contract, JvmBinding binding, Object target,
                                 SofaRuntimeContext sofaRuntimeContext) {

    }

    public BindingType getBindingType() {
        return JvmBinding.JVM_BINDING_TYPE;
    }

    public Class<JvmBinding> getBindingClass() {
        return JvmBinding.class;
    }

    public Object inBinding(Object contract, JvmBinding binding,
                            SofaRuntimeContext sofaRuntimeContext) {
        return createServiceProxy((Contract) contract, binding, sofaRuntimeContext);
    }

    @Override
    public void unInBinding(Object contract, JvmBinding binding,
                            SofaRuntimeContext sofaRuntimeContext) {
        binding.setDestroyed(true);
        if (binding.hasBackupProxy()) {
            binding.setBackupProxy(null);
        }
    }

    /**
     * create service proxy object
     *
     * @return proxy object
     */
    protected Object createServiceProxy(Contract contract, JvmBinding binding,
                                        SofaRuntimeContext sofaRuntimeContext) {

        ClassLoader newClassLoader;
        ClassLoader appClassLoader = sofaRuntimeContext.getAppClassLoader();
        Class<?> javaClass = contract.getInterfaceType();

        try {
            Class appLoadedClass = appClassLoader.loadClass(javaClass.getName());

            if (appLoadedClass == javaClass) {
                newClassLoader = appClassLoader;
            } else {
                newClassLoader = javaClass.getClassLoader();
            }
        } catch (ClassNotFoundException e) {
            newClassLoader = javaClass.getClassLoader();
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(newClassLoader);
            ServiceProxy handler = new JvmServiceInvoker(contract, binding, sofaRuntimeContext);
            ProxyFactory factory = new ProxyFactory();
            if (javaClass.isInterface()) {
                factory.addInterface(javaClass);
            } else {
                factory.setTargetClass(javaClass);
                factory.setProxyTargetClass(true);
            }
            factory.addAdvice(handler);
            return factory.getProxy();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * JVM Service Invoker
     */
    static class JvmServiceInvoker extends ServiceProxy {

        private Contract           contract;
        private JvmBinding         binding;
        private Object             target;
        private SofaRuntimeContext sofaRuntimeContext;

        public JvmServiceInvoker(Contract contract, JvmBinding binding,
                                 SofaRuntimeContext sofaRuntimeContext) {
            super(sofaRuntimeContext.getAppClassLoader());
            this.binding = binding;
            this.sofaRuntimeContext = sofaRuntimeContext;
            this.contract = contract;
        }

        @Override
        public Object doInvoke(MethodInvocation invocation) throws Throwable {
            if (binding.isDestroyed()) {
                throw new IllegalStateException("Can not call destroyed reference! JVM Reference[" +
                        getInterfaceName() + "#"
                        + getUniqueId() + "] has already been destroyed.");
            }

            SofaLogger
                    .debug(">> Start in JVM service invoke, the service interface is  - {0}"
                            , getInterfaceName());

            Object retVal;
            Object targetObj = this.getTarget();

            if ((targetObj == null || ((targetObj instanceof Proxy) && binding.hasBackupProxy()))) {
                targetObj = binding.getBackupProxy();
                SofaLogger
                    .debug("<<{0}.{1} backup proxy invoke.", getInterfaceName().getName(), invocation.getMethod()
                        .getName());
            }

            if (targetObj == null) {
                throw new IllegalStateException("JVM Reference["
                        +
                        getInterfaceName()
                        +
                        "#"
                        +
                        getUniqueId()
                        +
                        "] cant not find the corresponding JVM service. "
                        +
                        "Please check if there is a SOFA deployment publish the corresponding JVM service. "
                        +
                        "If this exception occurred when the application starts up, please add Require-Module to SOFA deployment's MANIFEST.MF to indicate the startup dependency of SOFA modules.");
            }

            ClassLoader tcl = Thread.currentThread().getContextClassLoader();
            try {
                pushThreadContextClassLoader(sofaRuntimeContext.getAppClassLoader());
                final Object finalTargetObj = targetObj;
                retVal = invocation.getMethod().invoke(finalTargetObj, invocation.getArguments());
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            } finally {
                SofaLogger
                    .debug("<< Finish JVM service invoke, the service implementation is  - {0}]"
                        , (this.target == null ? "null" : this.target.getClass().getName()));

                popThreadContextClassLoader(tcl);
            }

            return retVal;
        }

        @Override
        protected void do_catch(MethodInvocation invocation, Throwable e, long startTime) {
            if (SofaLogger.isDebugEnabled()) {
                SofaLogger.debug(getCommonInvocationLog("Exception", invocation,
                    startTime));
            }
        }

        @Override
        protected void do_finally(MethodInvocation invocation, long startTime) {
            if (SofaLogger.isDebugEnabled()) {
                SofaLogger.debug(getCommonInvocationLog("Finally", invocation,
                    startTime));
            }
        }

        protected Object getTarget() {
            if (this.target == null) {
                ComponentName componentName = ComponentNameFactory.createComponentName(
                    ServiceComponent.SERVICE_COMPONENT_TYPE, getInterfaceName(),
                    contract.getUniqueId());
                ComponentInfo componentInfo = sofaRuntimeContext.getComponentManager()
                    .getComponentInfo(componentName);

                if (componentInfo != null) {
                    this.target = componentInfo.getImplementation().getTarget();
                }
            }

            return target;
        }

        protected Class<?> getInterfaceName() {
            return contract.getInterfaceType();
        }

        protected String getUniqueId() {
            return contract.getUniqueId();
        }
    }
}