package xyz.haff.apisecurity.database

import io.kotest.core.Tuple2
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.dalesbred.Database
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.createDatabase

class UserRepositoryTest : FunSpec({

    listOf<Tuple2<(Database) -> UserRepository, String>>(
        Tuple2(::SafeUserRepository, "safe user repository"),
        Tuple2(::UnsafeUserRepository, "unsafe user repository"),
    ).forEach { (createRepository, repositoryName) ->
        test(repositoryName + ": correctly saves and finds user") {
            // ARRANGE
            val repository = createRepository(createDatabase(Config()))
            val userId = "userid"
            val hashedPassword = "hashedpassword"

            // ACT
            repository.save(userId, hashedPassword)
            val retrievedHashedPassword = repository.findHashedPassword(userId)

            // ASSERT
            retrievedHashedPassword shouldBe hashedPassword
        }
    }
})