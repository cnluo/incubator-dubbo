/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.utils.DefaultParameterNameReader;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.GeneralTypeConverter;

public class RestSpringScopeModelInitializer implements ScopeModelInitializer {
    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        beanFactory.registerBean(GeneralTypeConverter.class);
        beanFactory.registerBean(DefaultParameterNameReader.class);
        beanFactory.registerBean(CompositeArgumentResolver.class);
    }
}
