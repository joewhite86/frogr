Frogr - RESTful Graphdatabase Java framework
============================================

[![GitHub license](https://img.shields.io/github/license/joewhite86/frogr.svg)](https://github.com/joewhite86/frogr/blob/master/LICENSE)
[![Travis](https://img.shields.io/travis/joewhite86/frogr.svg)](https://travis-ci.org/joewhite86/frogr)
[![Read the Docs](https://img.shields.io/readthedocs/pip.svg)](http://frogr.readthedocs.io/en/latest/)
[![Twitter](https://img.shields.io/twitter/url/https/github.com/joewhite86/frogr.svg?style=social)](https://twitter.com/intent/tweet?text=Wow:&url=https%3A%2F%2Fgithub.com%2Fjoewhite86%2Ffrogr)

**Frogr is a Java framework for developing high-performance RESTful web services.**

With **Frogr** you can get a service up and running in minutes, but there’s no limit in complexity. 
**Frogr** uses a *repository pattern* for seperating data, business logic and external interfaces. 
This approach makes it easy to test and extend your code.

**Frogr** packs multiple stable libraries into an easy to use, light-weight framework, 
so you won't have tons of additional dependencies, you probably never will need.

Find the documentation at [frogr.readthedocs.io](http://frogr.readthedocs.io)

Example
-------
Java:
``` java

  PersonRepository repository = service().repository(Person.class);
  List<Person> persons = personRepository.search()
    .query("*Smith"))
    .fields("name", "children.name")
    .list()
```

REST:

``` REST
  
  GET localhost:8282/persons?q=*Smith&fields=name,children.name
  
 ```

``` json

  {
    "success": true,
    "data": [
      {
        "uuid": "99eaeddc012811e883fda165f97f8411",
        "type": "Person",
        "name": "Beth Smith",
        "children": [
          {
            "uuid": "99eb630e012811e883fd97343769a63e",
            "type": "Person",
            "name": "Morty Smith"
          },
          {
            "uuid": "99ebb12f012811e883fd2583ad59c919",
            "type": "Person",
            "name": "Summer Smith"
          }
        ]
      },
      {
        "uuid": "99eb3bfd012811e883fdd551cdefd5ed",
        "type": "Person",
        "name": "Jerry Smith",
        "children": [
          {
            "uuid": "99eb630e012811e883fd97343769a63e",
            "type": "Person",
            "name": "Morty Smith"
          },
          {
            "uuid": "99ebb12f012811e883fd2583ad59c919",
            "type": "Person",
            "name": "Summer Smith"
          }
        ]
      },
      {
        "uuid": "99eb630e012811e883fd97343769a63e",
        "type": "Person",
        "name": "Morty Smith",
        "children": []
      },
      {
        "uuid": "99ebb12f012811e883fd2583ad59c919",
        "type": "Person",
        "name": "Summer Smith",
        "children": []
      }
    ]
  }
 ```