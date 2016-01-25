package org.intermine.model;

import java.lang.Number;
import java.lang.Integer;

public class InterMineCoord extends Number {
  
  Integer value;
  public static int MAX_VALUE = Integer.MAX_VALUE;
  public static int MIN_VALUE = Integer.MIN_VALUE;
  public static int SIZE = Integer.SIZE;
  public static Class<Integer> TYPE = Integer.TYPE;
  public static String JDBC_TYPE = "integer";
  
  public InterMineCoord(Number i) {
    value = new Integer(i.intValue());
  }
  public InterMineCoord(String s) {
    value = new Integer(s);
  }
  public double doubleValue() {
    return value.doubleValue();
  }
  public float floatValue() {
    return value.floatValue();
  }
  public int intValue() {
    return value.intValue();
  }
  public byte byteValue() {
    return value.byteValue();
  }
  public short shortValue() {
    return value.shortValue();
  }
  public long longValue() {
    return value.longValue();
  }
  public String toString() {
    return value.toString();
  }
  public boolean equals(Object obj) {
    return value.equals(obj);
  }
  public int compareTo(InterMineCoord anotherInterMineCoord) {
    return value.compareTo(new Integer(anotherInterMineCoord.intValue()));
  }
  public int hashCode() {
    return value.hashCode();
  }
  public static Integer decode(String nm) {
    return Integer.decode(nm);
  }
  public static Integer getInteger(String nm) {
    return Integer.getInteger(nm);
  }
  public static Integer getInteger(String nm, Integer val) {
    return Integer.getInteger(nm,val);
  }
  public static Integer getInteger(String nm, int val) {
    return Integer.getInteger(nm,val);
  }
  public static InterMineCoord getInterMineCoord(String nm, InterMineCoord val) {
    return InterMineCoord.getInterMineCoord(nm,val);
  }
  public static InterMineCoord getInterMineCoord(String nm, int val) {
    return InterMineCoord.getInterMineCoord(nm,val);
  }
  public static InterMineCoord valueOf(String s) {
    return InterMineCoord.valueOf(Integer.parseInt(s));
  }
  public static InterMineCoord valueOf(String s, int radix) {
    return InterMineCoord.valueOf(Integer.valueOf(s,radix));
  }
  public static InterMineCoord valueOf(int i) {
    return new InterMineCoord(i);
  }
  public static InterMineCoord valueOf(InterMineCoord i) {
    return new InterMineCoord(i);
  }

  public static String toBinaryString(int i) {
    return Integer.toBinaryString(i);
  }
  public static String toHexString(int i) {
    return Integer.toHexString(i);
  }
  public static String toOctalString(int i) {
    return Integer.toOctalString(i);
  }
  public static String toString(int i) {
    return Integer.toString(i);
  }
  public static String toString(int i, int radix) {
    return Integer.toString(i,radix);
  }
  public static int bitCount(int i) {
    return Integer.bitCount(i);
  }
  public static int compare(int x, int y) {
    return Integer.compare(x,y);
  }
  public static int highestOneBit(int i) {
    return Integer.highestOneBit(i);
  }
  public static int lowestOneBit(int i) {
    return Integer.lowestOneBit(i);
  }
  public static int numberOfLeadingZeros(int i) {
    return Integer.numberOfLeadingZeros(i);
  }
  public static int numberOfTrailingZeros(int i) {
    return Integer.numberOfTrailingZeros(i);
  }
  public static int parseInt(String s) {
    return Integer.parseInt(s);
  }
  public static int parseInt(String s, int radix) {
    return Integer.parseInt(s,radix);
  }
  public static int reverse(int i) {
    return Integer.reverse(i);
  }
  public static int reverseBytes(int i) {
    return Integer.reverseBytes(i);
  }
  public static int rotateLeft(int i, int distance) {
    return Integer.rotateLeft(i,distance);
  }
  public static int rotateRight(int i, int distance) {
    return Integer.rotateRight(i,distance);
  }
  public static int signum(int i) {
    return Integer.signum(i);
  }

}
