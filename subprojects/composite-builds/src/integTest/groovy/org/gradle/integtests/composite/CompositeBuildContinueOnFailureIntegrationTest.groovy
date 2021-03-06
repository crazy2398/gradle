/*
 * Copyright 2017 the original author or authors.
 *
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

package org.gradle.integtests.composite

import groovy.transform.NotYetImplemented
import org.gradle.integtests.fixtures.build.BuildTestFile
import spock.lang.Ignore

/**
 * Tests for composite build delegating to tasks in an included build.
 */
class CompositeBuildContinueOnFailureIntegrationTest extends AbstractCompositeBuildIntegrationTest {
    BuildTestFile buildB
    BuildTestFile buildC

    def setup() {
        buildB = singleProjectBuild("buildB") {
            buildFile << """
                task fails {
                    doLast {
                        throw new RuntimeException("failed")
                    }
                }
                task succeeds {
                    shouldRunAfter fails
                }
                task checkContinueFlag {
                    doLast {
                        println "continueOnFailure = " + gradle.startParameter.continueOnFailure
                    }
                }
"""
        }
        buildC = singleProjectBuild("buildC") {
            buildFile << """
                task succeeds
"""
        }
        includedBuilds << buildB << buildC
    }

    def "aborts build when delegated task in same build fails"() {
        when:
        buildA.buildFile << """
    task delegate {
        dependsOn gradle.includedBuild('buildB').task(':fails')
        dependsOn gradle.includedBuild('buildB').task(':succeeds')
    }
"""

        fails(buildA, ":delegate")

        then:
        assertTaskExecuted(":buildB", ":fails")
        assertTaskNotExecuted(":buildB", ":succeeds")
    }

    // TODO:DAZ Fix this
    @Ignore("Currently if 'buildB' completes before 'buildC' starts, we don't continue: we don't yet handle --continue correctly with references")
    def "attempts all dependencies when run with --continue when one delegated task dependency fails"() {
        when:
        buildA.buildFile << """
    task delegate {
        dependsOn gradle.includedBuild('buildB').task(':fails')
        dependsOn gradle.includedBuild('buildC').task(':succeeds')
        dependsOn gradle.includedBuild('buildB').task(':succeeds')
    }
"""
        executer.withArguments("--continue")
        fails(buildA, ":delegate")

        then:
        assertTaskExecutedOnce(":buildB", ":fails")
        assertTaskExecutedOnce(":buildC", ":succeeds")
        assertTaskExecutedOnce(":buildB", ":succeeds")
    }

    @NotYetImplemented
    def "continues build when delegated task fails when run with --continue"() {
        when:
        buildA.buildFile << """
    task delegateWithFailure {
        dependsOn gradle.includedBuild('buildB').task(':fails')
    }
    task delegateWithSuccess {
        dependsOn gradle.includedBuild('buildB').task(':succeeds')
    }
    task delegate {
        dependsOn delegateWithSuccess, delegateWithFailure
    }
"""
        executer.withArguments("--continue")
        fails(buildA, ":delegate")

        then:
        // We attach the single failure in 'buildB' to every delegated task, so ':buildB:succeeds' appears to have failed
        // Thus ":delegateWithSuccess" is never executed.
        assertTaskExecutedOnce(":buildB", ":fails")
        assertTaskExecutedOnce(":buildB", ":succeeds")
        assertTaskExecutedOnce(":", ":delegateWithSuccess")
    }

    def "executes delegate task with --continue"() {
        when:
        buildB.buildFile << """
    task included {
        dependsOn 'fails', 'succeeds', 'checkContinueFlag'
    }
"""
        buildA.buildFile << """
    task delegate {
        dependsOn gradle.includedBuild('buildB').task(':included')
    }
"""
        executer.withArguments("--continue")
        fails(buildA, ":delegate")

        then:
        outputContains("continueOnFailure = true")

        assertTaskExecutedOnce(":buildB", ":checkContinueFlag")
        assertTaskExecutedOnce(":buildB", ":fails")
        assertTaskExecutedOnce(":buildB", ":succeeds")
    }

    def "passes continueOnFailure flag when building dependency artifact"() {
        when:
        buildB.buildFile << """
            apply plugin: 'java'
            
            jar.dependsOn 'checkContinueFlag'
"""
        dependency "org.test:buildB:1.0"

        executer.withArguments("--continue")
        execute(buildA, ":assemble")

        then:
        outputContains("continueOnFailure = true")

        assertTaskExecutedOnce(":buildB", ":checkContinueFlag")
        assertTaskExecutedOnce(":buildB", ":jar")
    }
}
