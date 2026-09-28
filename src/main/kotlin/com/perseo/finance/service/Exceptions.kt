package com.perseo.finance.service

class ResourceNotFoundException(message: String) : RuntimeException(message)
class DuplicateResourceException(message: String) : RuntimeException(message)
