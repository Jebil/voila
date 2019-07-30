package net.jk.app.commons.cucumber;

/** Common ThreadLocal variables, usually for performance reasons */
class ThreadLocals {

  /**
   * Thread-local StringBuilder, removes the overhead of creating a new one for every need Gets
   * reset to empty on every get() for easy usage in ad-hoc String concatenations
   */
  public static ThreadLocal<StringBuilder> STRINGBUILDER =
      new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
          return new StringBuilder(1024);
        }

        @Override
        public StringBuilder get() {
          StringBuilder bld = super.get();
          bld.setLength(0);
          return bld;
        }
      };
}
