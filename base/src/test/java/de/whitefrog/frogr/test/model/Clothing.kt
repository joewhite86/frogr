package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.Entity

class Clothing(var name: String? = null) : Entity(), InventoryItem {
  override var test: String? = null
}