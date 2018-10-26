package de.whitefrog.frogr.client

import de.whitefrog.frogr.model.BaseUser
import de.whitefrog.frogr.client.exception.ClientException
import de.whitefrog.frogr.client.exception.UnauthorizedException
import de.whitefrog.frogr.client.test.AuthTest
import de.whitefrog.frogr.client.test.model.User
import de.whitefrog.frogr.exception.DuplicateEntryException
import de.whitefrog.frogr.model.Filter
import de.whitefrog.frogr.model.SearchParameter
import org.junit.Assert.*
import org.junit.Test
import javax.ws.rs.NotFoundException

class TestClient: AuthTest() {
  private val url = "http://localhost:8282"
  
  @Test
  fun login() {
    val client = FrogrClient(url)
    val user = client.login("rick", "morty")
    assertNotNull(user)
  }
  
  @Test(expected = UnauthorizedException::class)
  fun logout() {
    val client = FrogrClient(url)
    val user = client.login("rick", "morty")
    
    client.logout(user)
    client.create(User())
  }
  
  @Test(expected = UnauthorizedException::class)
  fun unauthorized() {
    val client = FrogrClient(url)
    client.create(User())
  }
  
  @Test
  fun create() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    var user = User()
    user.login = "beth"
    user.password = "morty"
    
    val response = client.create(user)
    user = response.data[0]
    assertNotNull(user)
    assertEquals("beth", user.login)
    assertNotEquals("morty", user.password)
  }

  @Test(expected = ClientException::class)
  fun createInvalid() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val user = User()
    user.login = "beth"
    user.password = "morty"
    user.field = "too_long_string"
    client.create(user)
  }
  
  @Test(expected = DuplicateEntryException::class)
  fun createDuplicate() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val user = User()
    user.login = "beth"
    user.password = "morty"
    
    client.create(user)
    client.create(user)
  }
  
  @Test(expected = DuplicateEntryException::class)
  fun createConstraintViolation() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val user = User()
    user.login = "beth"
    user.password = "morty"
    user.uniqueField = "unique"
    
    val user2 = User()
    user2.login = "jerry"
    user2.password = "morty"
    user2.uniqueField = "unique"
    
    client.create(user, user2)
  }
  
  @Test
  fun update() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    var user = User()
    user.login = "justin"
    user.password = "morty"
    
    var response = client.create(user)
    user = response.data[0]
    user.login = "summer"
    
    response = client.update(user)
    user = response.data[0]
    assertNotNull(user)
    assertEquals("summer", user.login)
  }
  
  @Test(expected = NotFoundException::class)
  fun updateNotFound() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val user = User()
    user.uuid = "123123123"
    user.login = "justin"
    user.password = "morty"
    
    client.update(user)
  }

  @Test(expected = DuplicateEntryException::class)
  fun updateConstraintViolation() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val user = User()
    user.login = "beth"
    user.password = "morty"
    
    val user2 = User()
    user2.login = "jerry"
    user2.password = "morty"
    
    val response = client.create(user, user2)
    response.data.forEach { 
      it.uniqueField = "unique"
    }
    client.update(response.data[0], response.data[1])
  }
  
  @Test
  fun search() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    (1..5).forEach { 
      val user = User()
      user.login = "testSearch$it"
      user.password = "test"
      client.create(user)
    }
    val params = SearchParameter()
      .filter(Filter.StartsWith(BaseUser.Login, "testSearch"))
      .orderBy(BaseUser.Login)
    
    val response = client.search(User::class.java, params)
    assertEquals(5, response.data.size)
    assertEquals("testSearch1", response.data[0].login)
  }
  
  @Test(expected = ClientException::class)
  fun searchInvalid() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val params = SearchParameter()
      .filter(Filter.Equals("notExistantField", "test"))

    client.search(User::class.java, params)
  }
  
  @Test
  fun delete() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val user = User()
    user.login = "testDelete"
    user.password = "test"
    
    val response = client.create(user)
    assertTrue(response.data.isNotEmpty())
    client.delete(response.data[0])
  }
  
  @Test(expected = NotFoundException::class)
  fun deleteNotFound() {
    val client = FrogrClient(url)
    client.login("rick", "morty")
    
    val user = User()
    user.uuid = "123123123"
    user.login = "testDelete"
    user.password = "test"
    
    client.delete(user)
  }
}