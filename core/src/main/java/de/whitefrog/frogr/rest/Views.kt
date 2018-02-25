package de.whitefrog.frogr.rest

/**
 * Visibility classes for REST services.
 * Public is visibly to everyone.
 * Secure is only visible to the owning user itself.
 * Hidden is not available with REST.
 */
class Views {
  open class Public
  open class Secure : Public()
  class Hidden : Secure()
}
