public enum Ports {
   RES_REP(4990),
   FOR_PARTICLE(4991),
   FOR_SPRITE(4992);

   private int portNumber;

   Ports(int portNumber) {
      this.portNumber = portNumber;
   }

   public int getPortNumber() {
      return portNumber;
   }
}
