/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.codegen.process.persistence;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.junit.jupiter.api.Test;
import org.kie.kogito.codegen.api.AddonsConfig;
import org.kie.kogito.codegen.api.GeneratedFile;
import org.kie.kogito.codegen.api.GeneratedFileType;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.core.context.QuarkusKogitoBuildContext;
import org.kie.kogito.codegen.data.Person;
import org.kie.kogito.codegen.process.persistence.proto.ReflectionProtoGenerator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.kie.kogito.codegen.process.persistence.PersistenceGenerator.MONGODB_PERSISTENCE_TYPE;

class MongoDBPersistenceGeneratorTest {

    private static final String TEST_RESOURCES = "src/test/resources";

    private static final String PERSISTENCE_FILE_PATH = "org/kie/kogito/persistence/KogitoProcessInstancesFactoryImpl.java";
    private static final String TRANSACTION_FILE_PATH = "org/kie/kogito/persistence/transaction/TransactionExecutorImpl.java";

    @Test
    void test_noTransaction() {
        KogitoBuildContext context = QuarkusKogitoBuildContext.builder()
                .withApplicationProperties(new File(TEST_RESOURCES))
                .withPackageName(this.getClass().getPackage().getName())
                .withAddonsConfig(AddonsConfig.builder().withPersistence(true).build())
                .build();
        context.setApplicationProperty("kogito.persistence.type", MONGODB_PERSISTENCE_TYPE);

        ReflectionProtoGenerator protoGenerator = ReflectionProtoGenerator.builder().build(Collections.singleton(Person.class));
        PersistenceGenerator persistenceGenerator = new PersistenceGenerator(context, protoGenerator);

        Collection<GeneratedFile> generatedFiles = persistenceGenerator.generate();

        assertPersistenceFileGenerated(generatedFiles, "null");

        Optional<GeneratedFile> generatedCLASSFile = generatedFiles.stream().filter(gf -> gf.category() == GeneratedFileType.SOURCE.category())
                .filter(f -> TRANSACTION_FILE_PATH.equals(f.relativePath())).findAny();
        assertFalse(generatedCLASSFile.isPresent());
    }

    @Test
    void test_withTransaction() {
        KogitoBuildContext context = QuarkusKogitoBuildContext.builder()
                .withApplicationProperties(new File(TEST_RESOURCES))
                .withPackageName(this.getClass().getPackage().getName())
                .withAddonsConfig(AddonsConfig.builder().withPersistence(true).withTransaction(true).build())
                .build();
        context.setApplicationProperty("kogito.persistence.type", MONGODB_PERSISTENCE_TYPE);

        ReflectionProtoGenerator protoGenerator = ReflectionProtoGenerator.builder().build(Collections.singleton(Person.class));
        PersistenceGenerator persistenceGenerator = new PersistenceGenerator(context, protoGenerator);

        Collection<GeneratedFile> generatedFiles = persistenceGenerator.generate();

        assertPersistenceFileGenerated(generatedFiles, "transactionExecutor");

        Optional<GeneratedFile> generatedCLASSFile = generatedFiles.stream().filter(gf -> gf.category() == GeneratedFileType.SOURCE.category())
                .filter(f -> TRANSACTION_FILE_PATH.equals(f.relativePath())).findAny();
        assertTrue(generatedCLASSFile.isPresent());

        final CompilationUnit compilationUnit = parse(new ByteArrayInputStream(generatedCLASSFile.get().contents()));
        final ClassOrInterfaceDeclaration classDeclaration = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow(() -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));
        assertNotNull(classDeclaration);

        final List<ConstructorDeclaration> constructorDeclarations = compilationUnit.findAll(ConstructorDeclaration.class);
        assertEquals(2, constructorDeclarations.size());

        Optional<ConstructorDeclaration> annotatedConstructorDeclaration = constructorDeclarations.stream()
                .filter(c -> c.isAnnotationPresent("Inject")).findAny();
        assertTrue(annotatedConstructorDeclaration.isPresent());
    }

    void assertPersistenceFileGenerated(Collection<GeneratedFile> generatedFiles, String transactionMethodReturn) {
        Optional<GeneratedFile> generatedCLASSFile = generatedFiles.stream().filter(gf -> gf.category() == GeneratedFileType.SOURCE.category())
                .filter(f -> PERSISTENCE_FILE_PATH.equals(f.relativePath())).findAny();
        assertTrue(generatedCLASSFile.isPresent());
        GeneratedFile classFile = generatedCLASSFile.get();

        final CompilationUnit compilationUnit = parse(new ByteArrayInputStream(classFile.contents()));

        final ClassOrInterfaceDeclaration classDeclaration = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow(() -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

        assertNotNull(classDeclaration);

        final MethodDeclaration dbNameMethodDeclaration = classDeclaration.findFirst(MethodDeclaration.class, d -> d.getName().getIdentifier().equals("dbName")).orElseThrow(() -> new NoSuchElementException("Class declaration doesn't contain a method named \"dbName\"!"));
        assertNotNull(dbNameMethodDeclaration);
        assertTrue(dbNameMethodDeclaration.getBody().isPresent());

        final BlockStmt dbNameMethodBody = dbNameMethodDeclaration.getBody().get();
        assertThat(dbNameMethodBody.getStatements().size()).isOne();
        assertTrue(dbNameMethodBody.getStatements().get(0).isReturnStmt());

        final ReturnStmt dbNameReturnStmt = (ReturnStmt) dbNameMethodBody.getStatements().get(0);
        assertThat(dbNameReturnStmt.toString()).contains("kogito");

        final MethodDeclaration transactionMethodDeclaration = classDeclaration.findFirst(MethodDeclaration.class, d -> "transactionExecutor".equals(d.getName().getIdentifier())).orElseThrow(() -> new NoSuchElementException("Class declaration doesn't contain a method named \"transactionExecutor\"!"));
        assertNotNull(transactionMethodDeclaration);
        assertTrue(transactionMethodDeclaration.getBody().isPresent());

        final BlockStmt transactionMethodBody = transactionMethodDeclaration.getBody().get();
        assertThat(transactionMethodBody.getStatements().size()).isOne();
        assertTrue(transactionMethodBody.getStatements().get(0).isReturnStmt());

        final ReturnStmt transactionReturnStmt = (ReturnStmt) transactionMethodBody.getStatements().get(0);
        assertThat(transactionReturnStmt.toString()).contains(transactionMethodReturn);
    }
}
