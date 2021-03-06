/*
 * Copyright 2019 MovingBlocks
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
package org.terasology.logic.health;

import com.google.api.client.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.event.BeforeDamagedEvent;
import org.terasology.logic.health.event.DoDamageEvent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.moduletestingenvironment.TestEventReceiver;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DamageEventTest extends ModuleTestingEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(DamageEventTest.class);

    private EntityManager entityManager;

    @Override
    public Set<String> getDependencies() {
        Set<String> modules = Sets.newHashSet();
        modules.add("Health");
        return modules;
    }

    @Before
    public void initialize() {
        entityManager = getHostContext().get(EntityManager.class);
    }

    @Test
    public void damageTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoDamageEvent(10));
        assertEquals(player.getComponent(HealthComponent.class).currentHealth, 40);

        player.send(new DoDamageEvent(999));
        assertEquals(player.getComponent(HealthComponent.class).currentHealth, 0);
    }

    @Test
    public void damageEventSentTest() {
        TestEventReceiver<BeforeDamagedEvent> receiver = new TestEventReceiver<>(getHostContext(), BeforeDamagedEvent.class);
        List<BeforeDamagedEvent> list = receiver.getEvents();
        assertTrue(list.isEmpty());

        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoDamageEvent(10));
        assertEquals(1, list.size());
    }

    @Test
    public void damageModifyEventTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        TestEventReceiver<BeforeDamagedEvent> receiver = new TestEventReceiver<>(getHostContext(),
                BeforeDamagedEvent.class,
                (event, entity) -> {
                    event.add(5);
                    event.multiply(2);
                });
        // Expected damage value is ( initial:10 + 5 ) * 2 = 30
        // Expected health value is ( 50 - 30 ) == 20
        player.send(new DoDamageEvent(10));
        assertEquals(20, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void damageEventCancelTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        TestEventReceiver<BeforeDamagedEvent> receiver = new TestEventReceiver<>(getHostContext(),
                BeforeDamagedEvent.class,
                (event, entity) -> {
                    event.consume();
                });
        player.send(new DoDamageEvent(10));
        assertEquals(50, player.getComponent(HealthComponent.class).currentHealth);
    }

    @Test
    public void damageNegativeTest() {
        HealthComponent healthComponent = new HealthComponent();
        healthComponent.currentHealth = 50;
        healthComponent.maxHealth = 100;

        final EntityRef player = entityManager.create();
        player.addComponent(new PlayerCharacterComponent());
        player.addComponent(healthComponent);

        player.send(new DoDamageEvent(-10));

        // Negative base value are ignored by Damage system
        assertEquals(50, player.getComponent(HealthComponent.class).currentHealth);
    }

}
