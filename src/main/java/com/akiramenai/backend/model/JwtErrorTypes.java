package com.akiramenai.backend.model;

public enum JwtErrorTypes {
  JwtExpiredException,
  JwtSignatureException,
  JwtMalformedException,
  JwtUnsupportedException,
  JwtIllegalArgumentException,
}
