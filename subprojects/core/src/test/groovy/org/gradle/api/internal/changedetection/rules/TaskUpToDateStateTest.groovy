/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.changedetection.rules

import org.gradle.api.internal.changedetection.state.FileCollectionSnapshot
import org.gradle.api.internal.changedetection.state.FileCollectionSnapshotter
import org.gradle.api.internal.changedetection.state.FileCollectionSnapshotterRegistry
import org.gradle.api.internal.changedetection.state.GenericFileCollectionSnapshotter
import org.gradle.api.internal.changedetection.state.TaskHistoryRepository
import org.gradle.api.internal.changedetection.state.ValueSnapshotter
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.internal.classloader.ClassLoaderHierarchyHasher
import spock.lang.Subject

@Subject(TaskUpToDateState)
class TaskUpToDateStateTest extends AbstractTaskStateChangesTest {

    def "constructor invokes snapshots" () {
        def classLoaderHierarchyHasher = Mock(ClassLoaderHierarchyHasher)
        def stubHistory = Stub(TaskHistoryRepository.History)
        def fileCollectionFactory = Mock(FileCollectionFactory)
        def stubSnapshot = Stub(FileCollectionSnapshot)
        def mockInputFileSnapshotter = Mock(FileCollectionSnapshotter)
        def mockInputFileSnapshotterRegistry = Mock(FileCollectionSnapshotterRegistry)

        when:
        new TaskUpToDateState(stubTask, stubHistory, mockInputFileSnapshotterRegistry, fileCollectionFactory, classLoaderHierarchyHasher, new ValueSnapshotter(classLoaderHierarchyHasher))

        then:
        noExceptionThrown()
        1 * mockInputs.getProperties() >> [:]
        1 * mockInputs.getFileProperties() >> fileProperties(prop: "a")
        1 * mockOutputs.getFileProperties() >> fileProperties(out: "b")
        (1.._) * mockInputFileSnapshotterRegistry.getSnapshotter(GenericFileCollectionSnapshotter) >> mockInputFileSnapshotter
        (1.._) * mockInputFileSnapshotter.snapshot(_, _, _, _) >> stubSnapshot
    }
}
