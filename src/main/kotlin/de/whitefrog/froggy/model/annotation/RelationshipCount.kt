package de.whitefrog.froggy.model.annotation

import de.whitefrog.froggy.model.Model
import org.neo4j.graphdb.Direction
import kotlin.reflect.KClass

/**
 * Indicates that the field should contain the relationship count for a specified relationship type when fetched.
 * Will not be persisted.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class RelationshipCount(val type: String = "None", val direction: Direction = Direction.OUTGOING, val otherModel: KClass<out Model> = Model::class)
