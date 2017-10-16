package de.whitefrog.froggy.model.annotation

import de.whitefrog.froggy.model.Model
import org.neo4j.graphdb.Direction
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class RelationshipCount(val type: String = "None", val direction: Direction = Direction.OUTGOING, val otherModel: KClass<out Model> = Model::class)
