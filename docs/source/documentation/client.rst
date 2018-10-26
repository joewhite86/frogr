Client
======

Create a client:

.. code-block:: kotlin

  val client = FrogrClient("http://localhost:8282")

Login if authentication is enabled:

.. code-block:: kotlin

  val user = client.login("rick", "morty")

Create a model:

.. code-block:: kotlin

  var user = User()
  user.login = "beth"
  user.password = "morty"
  val response = client.create(user)

Update a model:

.. code-block:: kotlin

  user.name = "Rick"
  val response = client.update(user)

Search models:

.. code-block:: kotlin

  val params = SearchParameter()
    .filter(Filter.StartsWith(BaseUser.Login, "r"))
    .orderBy(BaseUser.Login)
    
  val response = client.search(User::class.java, params)

Delete a model:

.. code-block:: kotlin

  client.delete(user)
