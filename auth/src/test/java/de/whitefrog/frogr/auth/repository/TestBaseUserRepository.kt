package de.whitefrog.frogr.auth.repository

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.auth.exception.AuthenticationException
import de.whitefrog.frogr.auth.model.Role
import de.whitefrog.frogr.auth.test.model.User
import de.whitefrog.frogr.auth.test.repository.UserRepository
import de.whitefrog.frogr.exception.MissingRequiredException
import de.whitefrog.frogr.test.TemporaryService
import org.junit.AfterClass
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import javax.validation.ConstraintViolationException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestBaseUserRepository {
  companion object {
    private lateinit var service: Service
    private lateinit var users: UserRepository

    @BeforeClass
    @JvmStatic
    fun before() {
      service = TemporaryService()
      service.connect()
      users = service.repository(User::class.java)
    }

    @AfterClass
    @JvmStatic
    fun after() {
      service.shutdown()
    }
  }
  
  @Test
  fun register() {
    service.beginTx().use {
      val user = User()
      user.login = "test"
      user.password = "test"
      users.register(user)
      assertTrue(user.persisted)
      assertEquals(Role.User, user.role)
    }
  }

  @Test(expected = MissingRequiredException::class)
  fun missingPassword() {
    service.beginTx().use {
      val user = User()
      user.login = "test"
      users.register(user)
    }
  }
  
  @Test
  fun login() {
    service.beginTx().use {
      val user = User()
      user.login = "test"
      user.password = "test"
      users.register(user)

      val loginUser = users.login("test", "test")
      assertNotNull(loginUser)
      assertNotNull(loginUser.accessToken)
      assertEquals("test", user.login)
    }
  }

  @Test(expected = AuthenticationException::class)
  fun wrongPassword() {
    service.beginTx().use {
      val user = User()
      user.login = "test"
      user.password = "test"
      users.register(user)

      users.login("test", "xyz")
    }
  }

  @Test(expected = ConstraintViolationException::class)
  fun constraintViolation() {
    service.beginTx().use {
      val user = User()
      user.login = "test"
      user.password = "test"
      user.field = "tooLongString"
      users.register(user)
    }
  }

  @Test
  fun logout() {
    service.beginTx().use {
      val user = User()
      user.login = "test"
      user.password = "test"
      users.register(user)

      val loginUser = users.login("test", "test")
      assertNotNull(loginUser)
      assertNotNull(loginUser.accessToken)
      users.logout(loginUser)
      assertNull(loginUser.accessToken)
    }
  }
}
