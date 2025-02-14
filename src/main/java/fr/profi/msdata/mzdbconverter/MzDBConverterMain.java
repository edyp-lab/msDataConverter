package fr.profi.msdata.mzdbconverter;

import fr.profi.msdata.timstof.Timstof2Mzdb;


/**
 *
 */
public class MzDBConverterMain {

    private static final String ThermoFormat = "thermo";
    private static final String BruckerFormat = "brucker";

    public static void main(String[] argv) {
        //Get input file format to call corresponding converter
        if(argv == null||argv.length <2){
            printBasicUsage();
            System.exit(1);
        }

        String[] newArgs = new String[argv.length-1];
        System.arraycopy(argv,1,newArgs, 0,argv.length-1);

        switch (argv[0]){
            case ThermoFormat:
                Thermo2Mzdb.main(newArgs);
                break;
            case BruckerFormat:
                Timstof2Mzdb.main(newArgs);
                break;
        }

    }

    public static void  printBasicUsage(){
        System.out.println(" ----------- MzDBConverterMain Usage : ------------- ");
        System.out.println(" mzdbConverter.bat "+ThermoFormat+" XXXX : convert Thermo raw file to mzdb. To get specific option run ");
        System.out.println(" mzdbConverter.bat "+ThermoFormat+" --help");
        System.out.println(" mzdbConverter.bat "+BruckerFormat+" XXXX : convert Brucker .d file to mzdb. To get specific option run ");
        System.out.println(" mzdbConverter.bat "+BruckerFormat+" --help");
    }



}
