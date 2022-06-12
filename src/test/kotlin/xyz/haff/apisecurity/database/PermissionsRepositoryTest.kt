package xyz.haff.apisecurity.database

import io.kotest.core.Tuple2
import io.kotest.core.Tuple3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.dalesbred.Database
import org.dalesbred.integration.kotlin.findOptional
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.createDatabase

class PermissionsRepositoryTest : FunSpec({

    listOf<Tuple2<(Database) -> PermissionsRepository, String>>(
        Tuple2(::SafePermissionsRepository, "safe permissions repository"),
        Tuple2(::UnsafePermissionsRepository, "unsafe permissions repository"),
    ).forEach { (createRepository, repositoryName) ->
        test(repositoryName + ": correctly saves and finds permissions") {
            // ARRANGE
            val db = createDatabase()
            val repository = createRepository(db)

            val (spaceId, userId, permissions) = Tuple3(1L, "owner", "rwd")
            // To satisfy the foreign keys
            db.updateUnique("INSERT INTO spaces VALUES ( $spaceId, 'test', '$userId' )")
            db.updateUnique("INSERT INTO users VALUES ( '$userId', 'test' )")


            // ACT
            repository.save(spaceId, userId, permissions)
            val retrievedPermissions = repository.find(spaceId, userId)

            // ASSERT
            retrievedPermissions shouldBe permissions
        }
    }
})