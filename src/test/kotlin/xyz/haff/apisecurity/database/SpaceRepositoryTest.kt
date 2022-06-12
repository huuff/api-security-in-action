package xyz.haff.apisecurity.database

import io.kotest.core.Tuple2
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.dalesbred.Database
import org.dalesbred.integration.kotlin.findOptional
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.createDatabase

class SpaceRepositoryTest : FunSpec({

    listOf<Tuple2<(Database) -> SpaceRepository, String>>(
        Tuple2(::SafeSpaceRepository, "safe space repository"),
        Tuple2(::UnsafeSpaceRepository, "unsafe space repository"),
    ).forEach { (createRepository, repositoryName) ->
        test(repositoryName + ": correctly saves space") {
            // ARRANGE
            val db = createDatabase(Config())
            val repository = createRepository(db)

            val spaceName = "space"
            val ownerName = "owner"

            // ACT
            val savedId = repository.save(name = spaceName, owner = ownerName)

            // ASSERT
            db.findOptional<String>("SELECT name FROM spaces WHERE space_id = $savedId").get() shouldBe spaceName
            db.findOptional<String>("SELECT owner FROM spaces WHERE space_id = $savedId").get() shouldBe ownerName
        }
    }
})