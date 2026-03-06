package com.blockreplace.core.blockdb;

import java.util.List;

public record PropertyDef(String name, String type, List<String> allowedValues) {}

