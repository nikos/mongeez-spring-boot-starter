/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hzpz.spring.boot.autoconfigure.mongeez;

import com.mongodb.Mongo;
import org.junit.After;
import org.junit.Test;
import org.mongeez.Mongeez;
import org.mongeez.MongoAuth;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

public class MongeezAutoConfigurationTests {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    private void registerAndRefresh(Class<?>... annotatedClasses) {
        this.context.register(annotatedClasses);
        this.context.refresh();

    }

    @Test
    public void shouldDoNothingIfNoMongoBean() {
        registerAndRefresh(MongeezAutoConfiguration.class);
        assertThat(this.context.getBeanNamesForType(Mongeez.class), emptyArray());
    }

    @Test
    public void shouldDoNothingIfDisabled() {
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.enabled:false");
        registerAndRefresh(MongoAutoConfiguration.class, MongeezAutoConfiguration.class);
        assumeThat(this.context.getBeanNamesForType(Mongo.class), not(emptyArray()));
        assertThat(this.context.getBeanNamesForType(Mongeez.class), emptyArray());
    }

    @Test
    public void shouldUseDatabaseFromMongoProperties() {
        String database = "foo";
        EnvironmentTestUtils.addEnvironment(this.context, "spring.data.mongodb.database:" + database);
        registerAndRefresh(DoNotExecuteMongeezPostProcessor.class,
                MongoAutoConfiguration.class, MongeezAutoConfiguration.class);
        Mongeez mongeez = this.context.getBean(Mongeez.class);
        Object mongeezDatabase = ReflectionTestUtils.getField(mongeez, "dbName");
        assertThat(mongeezDatabase.toString(), equalTo(database));
    }

    @Test
    public void shouldUseDatabaseOverrideFromMongeezProperties() {
        String mongoDatabase = "foo";
        String mongeezOverrideDatabase = "bar";
        EnvironmentTestUtils.addEnvironment(this.context, "spring.data.mongodb.database:" + mongoDatabase);
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.database:" + mongeezOverrideDatabase);
        registerAndRefresh(DoNotExecuteMongeezPostProcessor.class,
                MongoAutoConfiguration.class, MongeezAutoConfiguration.class);
        Mongeez mongeez = this.context.getBean(Mongeez.class);
        Object mongeezActualDatabase = ReflectionTestUtils.getField(mongeez, "dbName");
        assertThat(mongeezActualDatabase.toString(), equalTo(mongeezOverrideDatabase));
    }

    @Test
    public void shouldUseAuthenticationDatabaseFromMongoProperties() {
        String database = "foo";
        EnvironmentTestUtils.addEnvironment(this.context, "spring.data.mongodb.authenticationDatabase:" + database);
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.username:user");
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.password:pass");
        registerAndRefresh(DoNotExecuteMongeezPostProcessor.class,
                MongoAutoConfiguration.class, MongeezAutoConfiguration.class);
        Mongeez mongeez = this.context.getBean(Mongeez.class);
        String authDb = ((MongoAuth) ReflectionTestUtils.getField(mongeez, "auth")).getAuthDb();
        assertThat(authDb, equalTo(database));
    }

    @Test
    public void shouldUseAuthenticationDatabaseOverrideFromMongeezProperties() {
        String database = "foo";
        String mongeezOverrideDatabase = "bar";
        EnvironmentTestUtils.addEnvironment(this.context, "spring.data.mongodb.authenticationDatabase:" + database);
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.authenticationDatabase:" + mongeezOverrideDatabase);
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.username:user");
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.password:pass");
        registerAndRefresh(DoNotExecuteMongeezPostProcessor.class,
                MongoAutoConfiguration.class, MongeezAutoConfiguration.class);
        Mongeez mongeez = this.context.getBean(Mongeez.class);
        String authDb = ((MongoAuth) ReflectionTestUtils.getField(mongeez, "auth")).getAuthDb();
        assertThat(authDb, equalTo(mongeezOverrideDatabase));
    }

    @Test(expected = BeanCreationException.class)
    public void shouldFailIfOnlyMongoCredentialsProvided() {
        String mongoUsername = "foo";
        String mongoPassword = "bar";
        EnvironmentTestUtils.addEnvironment(this.context, "spring.data.mongodb.username:" + mongoUsername);
        EnvironmentTestUtils.addEnvironment(this.context, "spring.data.mongodb.password:" + mongoPassword);
        registerAndRefresh(DoNotExecuteMongeezPostProcessor.class,
                MongoAutoConfiguration.class, MongeezAutoConfiguration.class);
    }

    @Test(expected = BeanCreationException.class)
    public void shouldFailIfLocationDoesNotExist() {
        EnvironmentTestUtils.addEnvironment(this.context, "mongeez.location:does/not/exist");
        registerAndRefresh(DoNotExecuteMongeezPostProcessor.class,
                MongoAutoConfiguration.class, MongeezAutoConfiguration.class);
    }

}
