/**
 * Copyright (C) 2016 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.feature;

import cz.cvut.kbss.jopa.environment.OWLClassA;
import cz.cvut.kbss.jopa.environment.OWLClassC;
import cz.cvut.kbss.jopa.environment.OWLClassR;
import cz.cvut.kbss.jopa.environment.listener.AnotherListener;
import cz.cvut.kbss.jopa.environment.listener.ConcreteListener;
import cz.cvut.kbss.jopa.environment.listener.ParentListener;
import cz.cvut.kbss.jopa.environment.utils.Generators;
import cz.cvut.kbss.jopa.environment.utils.MetamodelMocks;
import cz.cvut.kbss.jopa.environment.utils.TestEnvironmentUtils;
import cz.cvut.kbss.jopa.model.EntityManagerImpl;
import cz.cvut.kbss.jopa.model.MetamodelImpl;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.sessions.*;
import cz.cvut.kbss.jopa.transactions.EntityTransaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies entity lifecycle listener behavior w.r.t. the JPA 2.1 spec.
 */
public class EntityLifecycleListenersTest {

    private Descriptor descriptor;

    @Mock
    private MetamodelImpl metamodelMock;

    @Mock
    private CloneBuilderImpl cloneBuilderMock;

    @Mock
    private ConnectionWrapper storageMock;

    @Mock
    private EntityManagerImpl emMock;

    private ParentListener parentListenerMock;
    private ConcreteListener concreteListenerMock;
    private AnotherListener anotherListenerMock;

    private UnitOfWorkImpl uow;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.descriptor = new EntityDescriptor();
        final ServerSessionStub serverSessionStub = spy(new ServerSessionStub(storageMock));
        when(serverSessionStub.getMetamodel()).thenReturn(metamodelMock);
        when(serverSessionStub.getLiveObjectCache()).thenReturn(mock(CacheManager.class));
        final EntityTransaction transactionMock = mock(EntityTransaction.class);
        when(emMock.getTransaction()).thenReturn(transactionMock);
        when(transactionMock.isActive()).thenReturn(true);
        final MetamodelMocks mocks = new MetamodelMocks();
        mocks.setMocks(metamodelMock);
        this.parentListenerMock = mocks.forOwlClassS().parentListener();
        this.concreteListenerMock = mocks.forOwlClassR().concreteListener();
        this.anotherListenerMock = mocks.forOwlClassR().anotherListener();
        uow = new UnitOfWorkImpl(serverSessionStub);
        uow.setEntityManager(emMock);
        TestEnvironmentUtils.setMock(uow, UnitOfWorkImpl.class.getDeclaredField("cloneBuilder"), cloneBuilderMock);
    }

    @Test
    public void prePersistLifecycleListenerIsCalledBeforeInstanceIsInsertedIntoPersistenceContext() {
        final OWLClassR rInstance = spy(new OWLClassR());
        doAnswer(invocationOnMock -> {
            final OWLClassR instance = (OWLClassR) invocationOnMock.getArguments()[1];
            instance.setUri(Generators.createIndividualIdentifier());
            return null;
        }).when(storageMock).persist(null, rInstance, descriptor);
        uow.registerNewObject(rInstance, descriptor);
        final InOrder inOrder = inOrder(rInstance, parentListenerMock, concreteListenerMock, anotherListenerMock,
                storageMock);
        inOrder.verify(parentListenerMock).prePersist(rInstance);
        inOrder.verify(concreteListenerMock).prePersist(rInstance);
        inOrder.verify(anotherListenerMock).prePersist(rInstance);
        inOrder.verify(rInstance).prePersist();
        inOrder.verify(storageMock).persist(any(Object.class), eq(rInstance), eq(descriptor));
    }

    @Test
    public void preRemoveEntityLifecycleListenerIsCalledBeforeInstanceIsRemovedFromPersistenceContext() throws
                                                                                                        Exception {
        final OWLClassR rOriginal = new OWLClassR(Generators.createIndividualIdentifier());
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rInstance);
        uow.registerExistingObject(rOriginal, descriptor);
        uow.removeObject(rInstance);
        final InOrder inOrder = inOrder(rInstance, concreteListenerMock, storageMock);
        inOrder.verify(concreteListenerMock).preRemove(rInstance);
        inOrder.verify(rInstance).preRemove();
        inOrder.verify(storageMock).remove(rInstance.getUri(), OWLClassR.class, descriptor);
    }

    @Test
    public void postPersistEntityLifecycleListenerIsCalledAfterStoragePersistOccurs() {
        final OWLClassR rInstance = spy(new OWLClassR());
        doAnswer(invocationOnMock -> {
            final OWLClassR instance = (OWLClassR) invocationOnMock.getArguments()[1];
            instance.setUri(Generators.createIndividualIdentifier());
            return null;
        }).when(storageMock).persist(null, rInstance, descriptor);
        uow.registerNewObject(rInstance, descriptor);
        final InOrder inOrder = inOrder(rInstance, concreteListenerMock, storageMock);
        inOrder.verify(storageMock).persist(any(Object.class), eq(rInstance), eq(descriptor));
        inOrder.verify(concreteListenerMock).postPersist(rInstance);
        inOrder.verify(rInstance).postPersist();
    }

    @Test
    public void postRemoveEntityLifecycleListenerIsCalledAfterStorageRemoveOccurs() throws Exception {
        final OWLClassR rOriginal = new OWLClassR(Generators.createIndividualIdentifier());
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rInstance);
        uow.registerExistingObject(rOriginal, descriptor);
        uow.removeObject(rInstance);
        final InOrder inOrder = inOrder(rInstance, concreteListenerMock, storageMock);
        inOrder.verify(storageMock).remove(rInstance.getUri(), OWLClassR.class, descriptor);
        inOrder.verify(concreteListenerMock).postRemove(rInstance);
        inOrder.verify(rInstance).postRemove();
    }

    @Test
    public void postLoadEntityLifecycleListenerIsCalledAfterInstanceIsLoadedIntoPersistenceContext() throws Exception {
        final OWLClassR rOriginal = new OWLClassR(Generators.createIndividualIdentifier());
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rInstance);
        when(storageMock.find(new LoadingParameters<>(OWLClassR.class, rOriginal.getUri(), descriptor)))
                .thenReturn(rOriginal);
        final OWLClassR result = uow.readObject(OWLClassR.class, rOriginal.getUri(), descriptor);
        assertSame(rInstance, result);
        final InOrder inOrder = inOrder(rInstance, concreteListenerMock);
        inOrder.verify(concreteListenerMock).postLoad(rInstance);
        inOrder.verify(rInstance).postLoad();
    }

    @Test
    public void postLoadEntityLifecycleListenerIsCalledAfterInstanceRefresh() throws Exception {
        final OWLClassR rOriginal = new OWLClassR(Generators.createIndividualIdentifier());
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rInstance);
        uow.registerExistingObject(rOriginal, descriptor);
        uow.revertObject(rInstance);
        final InOrder inOrder = inOrder(rInstance, concreteListenerMock);
        inOrder.verify(concreteListenerMock).postLoad(rInstance);
        inOrder.verify(rInstance).postLoad();
    }

    @Test
    public void preUpdateIsCalledBeforeFieldUpdateIsMergedIntoStorage() throws Exception {
        final OWLClassR rOriginal = new OWLClassR(Generators.createIndividualIdentifier());
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rInstance);

        uow.registerExistingObject(rOriginal, descriptor);
        rInstance.setStringAtt("Update");
        // Have to call it manually, aspects do not work here
        uow.attributeChanged(rInstance, OWLClassR.getStringAttField());
        final InOrder inOrder = inOrder(rInstance, concreteListenerMock, storageMock);
        inOrder.verify(concreteListenerMock).preUpdate(rInstance);
        inOrder.verify(rInstance).preUpdate();
        inOrder.verify(storageMock).merge(rInstance, OWLClassR.getStringAttField(), descriptor);
    }

    @Test
    public void preUpdateIsCalledBeforeStorageMergeWhenDetachedInstanceIsMergedIntoPersistenceContext() {
        final OWLClassR rOriginal = spy(new OWLClassR(Generators.createIndividualIdentifier()));
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(storageMock.contains(rInstance.getUri(), rInstance.getClass(), descriptor)).thenReturn(true);
        when(storageMock.find(any())).thenReturn(rOriginal);
        rInstance.setStringAtt("differentString");
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rOriginal);
        final OWLClassR merged = uow.mergeDetached(rInstance, descriptor);
        final InOrder inOrder = inOrder(merged, concreteListenerMock, storageMock);
        inOrder.verify(concreteListenerMock).preUpdate(merged);
        inOrder.verify(merged).preUpdate();
        inOrder.verify(storageMock, atLeastOnce()).merge(eq(merged), any(Field.class), eq(descriptor));
    }

    @Test
    public void preUpdateIsNotCalledWhenMergedEntityHasNoChangesComparedToStorageOriginal() {
        final OWLClassR rOriginal = spy(new OWLClassR(Generators.createIndividualIdentifier()));
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(storageMock.contains(rInstance.getUri(), rInstance.getClass(), descriptor)).thenReturn(true);
        when(storageMock.find(any())).thenReturn(rOriginal);
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rOriginal);
        final OWLClassR merged = uow.mergeDetached(rInstance, descriptor);
        verify(concreteListenerMock, never()).preUpdate(any());
        verify(merged, never()).preUpdate();
        verify(storageMock, never()).merge(eq(merged), any(Field.class), eq(descriptor));
    }

    @Test
    public void postUpdateIsCalledAfterFieldUpdateWasMergedIntoStorage() throws Exception {
        final OWLClassR rOriginal = new OWLClassR(Generators.createIndividualIdentifier());
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rInstance);

        uow.registerExistingObject(rOriginal, descriptor);
        rInstance.setStringAtt("Update");
        // Have to call it manually, aspects do not work here
        uow.attributeChanged(rInstance, OWLClassR.getStringAttField());
        final InOrder inOrder = inOrder(rInstance, concreteListenerMock, storageMock);
        inOrder.verify(storageMock).merge(rInstance, OWLClassR.getStringAttField(), descriptor);
        inOrder.verify(concreteListenerMock).postUpdate(rInstance);
        inOrder.verify(rInstance).postUpdate();
    }

    @Test
    public void postUpdateIsCalledAfterStorageMergeWhenDetachedInstanceIsMergedIntoPersistenceContext() {
        final OWLClassR rOriginal = spy(new OWLClassR(Generators.createIndividualIdentifier()));
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(storageMock.contains(rInstance.getUri(), rInstance.getClass(), descriptor)).thenReturn(true);
        when(storageMock.find(any())).thenReturn(rOriginal);
        rInstance.setStringAtt("differentString");
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rOriginal);
        final OWLClassR merged = uow.mergeDetached(rInstance, descriptor);
        final InOrder inOrder = inOrder(merged, concreteListenerMock, storageMock);
        inOrder.verify(storageMock, atLeastOnce()).merge(eq(merged), any(Field.class), eq(descriptor));
        inOrder.verify(concreteListenerMock).postUpdate(merged);
        inOrder.verify(merged).postUpdate();
    }

    @Test
    public void postUpdateIsNotCalledWhenMergedEntityHasNoChangesComparedToStorageOriginal() {
        final OWLClassR rOriginal = spy(new OWLClassR(Generators.createIndividualIdentifier()));
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        when(storageMock.contains(rInstance.getUri(), rInstance.getClass(), descriptor)).thenReturn(true);
        when(storageMock.find(any())).thenReturn(rOriginal);
        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenReturn(rOriginal);
        final OWLClassR merged = uow.mergeDetached(rInstance, descriptor);
        verify(concreteListenerMock, never()).postUpdate(any());
        verify(merged, never()).postUpdate();
        verify(storageMock, never()).merge(eq(merged), any(Field.class), eq(descriptor));
    }

    @Test
    public void postLoadListenerMethodsAreCalledOnReferencedEntitiesAsWell() {
        final OWLClassR rOriginal = spy(new OWLClassR(Generators.createIndividualIdentifier()));
        final OWLClassR rInstance = spy(new OWLClassR(rOriginal.getUri()));
        final OWLClassA aOriginal = spy(Generators.generateOwlClassAInstance());
        final OWLClassA aInstance = spy(new OWLClassA(aOriginal.getUri()));
        rInstance.setOwlClassA(aInstance);

        when(cloneBuilderMock.buildClone(eq(rOriginal), any())).thenAnswer(inv -> {
            final CloneConfiguration config = (CloneConfiguration) inv.getArguments()[1];
            uow.registerExistingObject(aOriginal, config.getDescriptor(), config.getPostRegister());
            return rInstance;
        });
        when(cloneBuilderMock.buildClone(eq(aOriginal), any())).thenReturn(aInstance);
        when(storageMock.find(new LoadingParameters<>(OWLClassR.class, rOriginal.getUri(), descriptor)))
                .thenReturn(rOriginal);
        final OWLClassR result = uow.readObject(OWLClassR.class, rOriginal.getUri(), descriptor);
        assertSame(rInstance, result);
        final InOrder inOrder = inOrder(rInstance, aInstance, concreteListenerMock);
        inOrder.verify(aInstance).postLoad();
        inOrder.verify(concreteListenerMock).postLoad(rInstance);
        inOrder.verify(rInstance).postLoad();
    }

    @Test
    public void postLoadListenersAreCalledOnPluralReferencesAsWell() {
        final OWLClassC cOriginal = new OWLClassC(Generators.createIndividualIdentifier());
        final OWLClassC cInstance = new OWLClassC(cOriginal.getUri());
        final OWLClassA aOriginal = spy(Generators.generateOwlClassAInstance());
        final OWLClassA aInstance = spy(new OWLClassA(aOriginal.getUri()));
        cOriginal.setSimpleList(Collections.singletonList(aOriginal));
        cInstance.setSimpleList(Collections.singletonList(aInstance));
        when(cloneBuilderMock.buildClone(eq(cOriginal), any())).thenAnswer(inv -> {
            final CloneConfiguration config = (CloneConfiguration) inv.getArguments()[1];
            uow.registerExistingObject(aOriginal, config.getDescriptor(), config.getPostRegister());
            return cInstance;
        });
        when(cloneBuilderMock.buildClone(eq(aOriginal), any())).thenReturn(aInstance);
        when(storageMock.find(new LoadingParameters<>(OWLClassC.class, cOriginal.getUri(), descriptor)))
                .thenReturn(cOriginal);
        final OWLClassC result = uow.readObject(OWLClassC.class, cOriginal.getUri(), descriptor);
        assertSame(cInstance, result);
        final InOrder inOrder = inOrder(aInstance, concreteListenerMock);
        inOrder.verify(aInstance).postLoad();
    }
}
