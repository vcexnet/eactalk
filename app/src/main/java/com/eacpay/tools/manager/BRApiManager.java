package com.eacpay.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import com.eacpay.EacApp;
import com.eacpay.presenter.entities.CurrencyEntity;
import com.eacpay.tools.sqlite.CurrencyDataSource;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.Utils;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class BRApiManager {
    private static BRApiManager instance;
    private Timer timer;

    private TimerTask timerTask;

    private final Handler handler;

    private BRApiManager() {
        handler = new Handler();
    }

    public static BRApiManager getInstance() {
        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context) {
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = fetchRates(context);
            updateFeePerKb(context);
            if (arr != null) {
                int length = arr.length();
                for (int i = 0; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("code");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = (float) tmpObj.getDouble("n");
                        String selectedISO = BRSharedPrefs.getIso(context);
                        if (tmp.code.equalsIgnoreCase(selectedISO)) {
                            BRSharedPrefs.putIso(context, tmp.code);
                            BRSharedPrefs.putCurrencyListPosition(context, i - 1);
                        }
                    } catch (JSONException e) {
                        Timber.e(e);
                    }
                    set.add(tmp);
                }
            } else {
                Timber.d("getCurrencies: failed to get currencies");
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        List tempList = new ArrayList<>(set);
        Collections.reverse(tempList);
        return new LinkedHashSet<>(set);
    }


    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (!EacApp.isAppInBackground(context)) {
                                    Timber.d("doInBackground: Stopping timer, no activity on.");
                                    BRApiManager.getInstance().stopTimerTask();
                                }
                                Set<CurrencyEntity> tmp = getCurrencies((Activity) context);
                                CurrencyDataSource.getInstance(context).putCurrencies(tmp);
                            }
                        });
                    }
                });
            }
        };
    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask(context);

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 60000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    public static JSONArray fetchRates(Activity activity) {
        String jsonString = urlGET(activity, "https://api.eactalk.com/api/v1/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            jsonArray = new JSONArray(jsonString);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray == null ? backupFetchRates(activity) : jsonArray;
    }

    public static JSONArray backupFetchRates(Activity activity) {
        String jsonString = urlGET(activity, "https://exapi.eacpay.com/api/v1/rates");
        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            jsonArray = new JSONArray(jsonString);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        if (jsonArray == null) {
            try {
                jsonArray = new JSONArray("\n" +
                        "[{\"code\":\"BIF\",\"price\":\"BIF2.313225\",\"name\":\"Burundian Franc\",\"n\":2.313225},{\"code\":\"MWK\",\"price\":\"MWK0.93092929985\",\"name\":\"Malawian Kwacha\",\"n\":0.93092929985},{\"code\":\"BYR\",\"price\":\"BYR22.540000\",\"name\":\"Belarusian Ruble\",\"n\":22.540000},{\"code\":\"BYN\",\"price\":\"BYN0.00375058355\",\"name\":\"New Belarusian Ruble\",\"n\":0.00375058355},{\"code\":\"HUF\",\"price\":\"HUF0.39958417960\",\"name\":\"Hungarian Forint\",\"n\":0.39958417960},{\"code\":\"AOA\",\"price\":\"AOA0.50448779715\",\"name\":\"Angolan Kwanza\",\"n\":0.50448779715},{\"code\":\"JPY\",\"price\":\"JPY0.14294937690\",\"name\":\"Japanese Yen\",\"n\":0.14294937690},{\"code\":\"MNT\",\"price\":\"MNT3.31058870965\",\"name\":\"Mongolian Tugrik\",\"n\":3.31058870965},{\"code\":\"PLN\",\"price\":\"PLN0.00489983260\",\"name\":\"Polish Zloty\",\"n\":0.00489983260},{\"code\":\"GBP\",\"price\":\"GBP0.00088337135\",\"name\":\"British Pound Sterling\",\"n\":0.00088337135},{\"code\":\"SBD\",\"price\":\"SBD0.00921112510\",\"name\":\"Solomon Islands Dollar\",\"n\":0.00921112510},{\"code\":\"AWG\",\"price\":\"AWG0.00206885\",\"name\":\"Aruban Florin\",\"n\":0.00206885},{\"code\":\"KRW\",\"price\":\"KRW1.41352829485\",\"name\":\"South Korean Won\",\"n\":1.41352829485},{\"code\":\"NPR\",\"price\":\"NPR0.13958609035\",\"name\":\"Nepalese Rupee\",\"n\":0.13958609035},{\"code\":\"INR\",\"price\":\"INR0.08730512960\",\"name\":\"Indian Rupee\",\"n\":0.08730512960},{\"code\":\"YER\",\"price\":\"YER0.28790291860\",\"name\":\"Yemeni Rial\",\"n\":0.28790291860},{\"code\":\"AFN\",\"price\":\"AFN0.10120042320\",\"name\":\"Afghan Afghani\",\"n\":0.10120042320},{\"code\":\"MVR\",\"price\":\"MVR0.01776793470\",\"name\":\"Maldivian Rufiyaa\",\"n\":0.01776793470},{\"code\":\"KZT\",\"price\":\"KZT0.51040655735\",\"name\":\"Kazakhstani Tenge\",\"n\":0.51040655735},{\"code\":\"SRD\",\"price\":\"SRD0.02382919370\",\"name\":\"Surinamese Dollar\",\"n\":0.02382919370},{\"code\":\"SZL\",\"price\":\"SZL0.0168709255\",\"name\":\"Swazi Lilangeni\",\"n\":0.0168709255},{\"code\":\"LTL\",\"price\":\"LTL0.0033956510\",\"name\":\"Lithuanian Litas\",\"n\":0.0033956510},{\"code\":\"SAR\",\"price\":\"SAR0.00431315435\",\"name\":\"Saudi Riyal\",\"n\":0.00431315435},{\"code\":\"TTD\",\"price\":\"TTD0.00780363205\",\"name\":\"Trinidad and Tobago Dollar\",\"n\":0.00780363205},{\"code\":\"BHD\",\"price\":\"BHD0.00043369490\",\"name\":\"Bahraini Dinar\",\"n\":0.00043369490},{\"code\":\"HTG\",\"price\":\"HTG0.12540237675\",\"name\":\"Haitian Gourde\",\"n\":0.12540237675},{\"code\":\"ANG\",\"price\":\"ANG0.00207350060\",\"name\":\"Netherlands Antillean Guilder\",\"n\":0.00207350060},{\"code\":\"PKR\",\"price\":\"PKR0.21421629370\",\"name\":\"Pakistani Rupee\",\"n\":0.21421629370},{\"code\":\"XCD\",\"price\":\"XCD0.0031079325\",\"name\":\"East Caribbean Dollar\",\"n\":0.0031079325},{\"code\":\"LKR\",\"price\":\"LKR0.36240165510\",\"name\":\"Sri Lankan Rupee\",\"n\":0.36240165510},{\"code\":\"NGN\",\"price\":\"NGN0.47799793355\",\"name\":\"Nigerian Naira\",\"n\":0.47799793355},{\"code\":\"CRC\",\"price\":\"CRC0.75502040085\",\"name\":\"Costa Rican Colón\",\"n\":0.75502040085},{\"code\":\"CZK\",\"price\":\"CZK0.02583647960\",\"name\":\"Czech Republic Koruna\",\"n\":0.02583647960},{\"code\":\"ZWL\",\"price\":\"ZWL0.37029953080\",\"name\":\"Zimbabwean Dollar\",\"n\":0.37029953080},{\"code\":\"GIP\",\"price\":\"GIP0.00088200285\",\"name\":\"Gibraltar Pound\",\"n\":0.00088200285},{\"code\":\"RON\",\"price\":\"RON0.00522491460\",\"name\":\"Romanian Leu\",\"n\":0.00522491460},{\"code\":\"MMK\",\"price\":\"MMK2.13011533690\",\"name\":\"Myanma Kyat\",\"n\":2.13011533690},{\"code\":\"MUR\",\"price\":\"MUR0.04962857430\",\"name\":\"Mauritian Rupee\",\"n\":0.04962857430},{\"code\":\"NOK\",\"price\":\"NOK0.0099899925\",\"name\":\"Norwegian Krone\",\"n\":0.0099899925},{\"code\":\"SYP\",\"price\":\"SYP2.88880038870\",\"name\":\"Syrian Pound\",\"n\":2.88880038870},{\"code\":\"IMP\",\"price\":\"IMP0.00088200285\",\"name\":\"Manx pound\",\"n\":0.00088200285},{\"code\":\"CAD\",\"price\":\"CAD0.0014451820\",\"name\":\"Canadian Dollar\",\"n\":0.0014451820},{\"code\":\"BGN\",\"price\":\"BGN0.00206822325\",\"name\":\"Bulgarian Lev\",\"n\":0.00206822325},{\"code\":\"RSD\",\"price\":\"RSD0.12447320090\",\"name\":\"Serbian Dinar\",\"n\":0.12447320090},{\"code\":\"DOP\",\"price\":\"DOP0.06330795195\",\"name\":\"Dominican Peso\",\"n\":0.06330795195},{\"code\":\"KMF\",\"price\":\"KMF0.52089294160\",\"name\":\"Comorian Franc\",\"n\":0.52089294160},{\"code\":\"CUP\",\"price\":\"CUP0.030475\",\"name\":\"Cuban Peso\",\"n\":0.030475},{\"code\":\"GMD\",\"price\":\"GMD0.06221943095\",\"name\":\"Gambian Dalasi\",\"n\":0.06221943095},{\"code\":\"TWD\",\"price\":\"TWD0.03324270960\",\"name\":\"New Taiwan Dollar\",\"n\":0.03324270960},{\"code\":\"IQD\",\"price\":\"IQD1.679000\",\"name\":\"Iraqi Dinar\",\"n\":1.679000},{\"code\":\"SDG\",\"price\":\"SDG0.51462922970\",\"name\":\"Sudanese Pound\",\"n\":0.51462922970},{\"code\":\"BSD\",\"price\":\"BSD0.00115046460\",\"name\":\"Bahamian Dollar\",\"n\":0.00115046460},{\"code\":\"SLL\",\"price\":\"SLL13.97250038985\",\"name\":\"Sierra Leonean Leone\",\"n\":13.97250038985},{\"code\":\"CUC\",\"price\":\"CUC0.001150\",\"name\":\"Cuban Convertible Peso\",\"n\":0.001150},{\"code\":\"ZAR\",\"price\":\"ZAR0.0166835675\",\"name\":\"South African Rand\",\"n\":0.0166835675},{\"code\":\"TND\",\"price\":\"TND0.00342474370\",\"name\":\"Tunisian Dinar\",\"n\":0.00342474370},{\"code\":\"CLP\",\"price\":\"CLP0.93725041515\",\"name\":\"Chilean Peso\",\"n\":0.93725041515},{\"code\":\"HNL\",\"price\":\"HNL0.02810644735\",\"name\":\"Honduran Lempira\",\"n\":0.02810644735},{\"code\":\"UGX\",\"price\":\"UGX4.07841218240\",\"name\":\"Ugandan Shilling\",\"n\":4.07841218240},{\"code\":\"MXN\",\"price\":\"MXN0.02304264775\",\"name\":\"Mexican Peso\",\"n\":0.02304264775},{\"code\":\"STD\",\"price\":\"STD23.80267815920\",\"name\":\"São Tomé and Príncipe Dobra\",\"n\":23.80267815920},{\"code\":\"LVL\",\"price\":\"LVL0.0006956235\",\"name\":\"Latvian Lats\",\"n\":0.0006956235},{\"code\":\"SCR\",\"price\":\"SCR0.01657915785\",\"name\":\"Seychellois Rupee\",\"n\":0.01657915785},{\"code\":\"CDF\",\"price\":\"CDF2.31265041630\",\"name\":\"Congolese Franc\",\"n\":2.31265041630},{\"code\":\"BBD\",\"price\":\"BBD0.00232291260\",\"name\":\"Barbadian Dollar\",\"n\":0.00232291260},{\"code\":\"GTQ\",\"price\":\"GTQ0.00883573175\",\"name\":\"Guatemalan Quetzal\",\"n\":0.00883573175},{\"code\":\"FJD\",\"price\":\"FJD0.00243512960\",\"name\":\"Fijian Dollar\",\"n\":0.00243512960},{\"code\":\"TMT\",\"price\":\"TMT0.0040365\",\"name\":\"Turkmenistani Manat\",\"n\":0.0040365},{\"code\":\"CLF\",\"price\":\"CLF0.00003396640\",\"name\":\"Chilean Unit of Account (UF)\",\"n\":0.00003396640},{\"code\":\"BRL\",\"price\":\"BRL0.00540454460\",\"name\":\"Brazilian Real\",\"n\":0.00540454460},{\"code\":\"PEN\",\"price\":\"PEN0.00427167960\",\"name\":\"Peruvian Nuevo Sol\",\"n\":0.00427167960},{\"code\":\"NZD\",\"price\":\"NZD0.00167684375\",\"name\":\"New Zealand Dollar\",\"n\":0.00167684375},{\"code\":\"WST\",\"price\":\"WST0.00301535290\",\"name\":\"Samoan Tala\",\"n\":0.00301535290},{\"code\":\"NIO\",\"price\":\"NIO0.04111829485\",\"name\":\"Nicaraguan Córdoba\",\"n\":0.04111829485},{\"code\":\"BAM\",\"price\":\"BAM0.00206698010\",\"name\":\"Bosnia-Herzegovina Convertible Mark\",\"n\":0.00206698010},{\"code\":\"EGP\",\"price\":\"EGP0.02109267210\",\"name\":\"Egyptian Pound\",\"n\":0.02109267210},{\"code\":\"MOP\",\"price\":\"MOP0.00928858335\",\"name\":\"Macanese Pataca\",\"n\":0.00928858335},{\"code\":\"NAD\",\"price\":\"NAD0.01687093355\",\"name\":\"Namibian Dollar\",\"n\":0.01687093355},{\"code\":\"BZD\",\"price\":\"BZD0.00231900145\",\"name\":\"Belize Dollar\",\"n\":0.00231900145},{\"code\":\"MGA\",\"price\":\"MGA4.60000039905\",\"name\":\"Malagasy Ariary\",\"n\":4.60000039905},{\"code\":\"XDR\",\"price\":\"XDR0.00083848915\",\"name\":\"Special Drawing Rights\",\"n\":0.00083848915},{\"code\":\"COP\",\"price\":\"COP4.3289910\",\"name\":\"Colombian Peso\",\"n\":4.3289910},{\"code\":\"RUB\",\"price\":\"RUB0.09228792895\",\"name\":\"Russian Ruble\",\"n\":0.09228792895},{\"code\":\"PYG\",\"price\":\"PYG7.89842945835\",\"name\":\"Paraguayan Guarani\",\"n\":7.89842945835},{\"code\":\"ISK\",\"price\":\"ISK0.14782144390\",\"name\":\"Icelandic Króna\",\"n\":0.14782144390},{\"code\":\"JMD\",\"price\":\"JMD0.17736987165\",\"name\":\"Jamaican Dollar\",\"n\":0.17736987165},{\"code\":\"LYD\",\"price\":\"LYD0.00537629485\",\"name\":\"Libyan Dinar\",\"n\":0.00537629485},{\"code\":\"BMD\",\"price\":\"BMD0.001150\",\"name\":\"Bermudan Dollar\",\"n\":0.001150},{\"code\":\"KWD\",\"price\":\"KWD0.0003506925\",\"name\":\"Kuwaiti Dinar\",\"n\":0.0003506925},{\"code\":\"PHP\",\"price\":\"PHP0.05931129485\",\"name\":\"Philippine Peso\",\"n\":0.05931129485},{\"code\":\"BDT\",\"price\":\"BDT0.09926348625\",\"name\":\"Bangladeshi Taka\",\"n\":0.09926348625},{\"code\":\"CNY\",\"price\":\"CNY0.00731986960\",\"name\":\"Chinese Yuan\",\"n\":0.00731986960},{\"code\":\"THB\",\"price\":\"THB0.03862049370\",\"name\":\"Thai Baht\",\"n\":0.03862049370},{\"code\":\"UZS\",\"price\":\"UZS13.05825038525\",\"name\":\"Uzbekistan Som\",\"n\":13.05825038525},{\"code\":\"XPF\",\"price\":\"XPF0.12667291860\",\"name\":\"CFP Franc\",\"n\":0.12667291860},{\"code\":\"MRO\",\"price\":\"MRO0.41054980220\",\"name\":\"Mauritanian Ouguiya\",\"n\":0.41054980220},{\"code\":\"IRR\",\"price\":\"IRR48.70250040480\",\"name\":\"Iranian Rial\",\"n\":48.70250040480},{\"code\":\"ARS\",\"price\":\"ARS0.12899009845\",\"name\":\"Argentine Peso\",\"n\":0.12899009845},{\"code\":\"QAR\",\"price\":\"QAR0.00418719370\",\"name\":\"Qatari Rial\",\"n\":0.00418719370},{\"code\":\"GNF\",\"price\":\"GNF10.25225040825\",\"name\":\"Guinean Franc\",\"n\":10.25225040825},{\"code\":\"ERN\",\"price\":\"ERN0.01725000575\",\"name\":\"Eritrean Nakfa\",\"n\":0.01725000575},{\"code\":\"MZN\",\"price\":\"MZN0.07340493355\",\"name\":\"Mozambican Metical\",\"n\":0.07340493355},{\"code\":\"SVC\",\"price\":\"SVC0.01006630420\",\"name\":\"Salvadoran Colón\",\"n\":0.01006630420},{\"code\":\"BTN\",\"price\":\"BTN0.0872409895\",\"name\":\"Bhutanese Ngultrum\",\"n\":0.0872409895},{\"code\":\"UAH\",\"price\":\"UAH0.03382415765\",\"name\":\"Ukrainian Hryvnia\",\"n\":0.03382415765},{\"code\":\"KES\",\"price\":\"KES0.13265294275\",\"name\":\"Kenyan Shilling\",\"n\":0.13265294275},{\"code\":\"SEK\",\"price\":\"SEK0.0108666260\",\"name\":\"Swedish Krona\",\"n\":0.0108666260},{\"code\":\"CVE\",\"price\":\"CVE0.11689795310\",\"name\":\"Cape Verdean Escudo\",\"n\":0.11689795310},{\"code\":\"AZN\",\"price\":\"AZN0.0019595655\",\"name\":\"Azerbaijani Manat\",\"n\":0.0019595655},{\"code\":\"TOP\",\"price\":\"TOP0.0025939975\",\"name\":\"Tongan Paʻanga\",\"n\":0.0025939975},{\"code\":\"OMR\",\"price\":\"OMR0.00044276610\",\"name\":\"Omani Rial\",\"n\":0.00044276610},{\"code\":\"PGK\",\"price\":\"PGK0.00404843125\",\"name\":\"Papua New Guinean Kina\",\"n\":0.00404843125},{\"code\":\"XOF\",\"price\":\"XOF0.69517914230\",\"name\":\"CFA Franc BCEAO\",\"n\":0.69517914230},{\"code\":\"GEL\",\"price\":\"GEL0.0035132960\",\"name\":\"Georgian Lari\",\"n\":0.0035132960},{\"code\":\"BTC\",\"price\":\"BTC2.695005680E-8\",\"name\":\"Bitcoin\",\"n\":2.695005680E-8},{\"code\":\"UYU\",\"price\":\"UYU0.04834891065\",\"name\":\"Uruguayan Peso\",\"n\":0.04834891065},{\"code\":\"MAD\",\"price\":\"MAD0.01123593815\",\"name\":\"Moroccan Dirham\",\"n\":0.01123593815},{\"code\":\"FKP\",\"price\":\"FKP0.00088200285\",\"name\":\"Falkland Islands Pound\",\"n\":0.00088200285},{\"code\":\"MYR\",\"price\":\"MYR0.00485472960\",\"name\":\"Malaysian Ringgit\",\"n\":0.00485472960},{\"code\":\"EUR\",\"price\":\"EUR0.0010573905\",\"name\":\"Euro\",\"n\":0.0010573905},{\"code\":\"LSL\",\"price\":\"LSL0.01687093930\",\"name\":\"Lesotho Loti\",\"n\":0.01687093930},{\"code\":\"DKK\",\"price\":\"DKK0.00786404960\",\"name\":\"Danish Krone\",\"n\":0.00786404960},{\"code\":\"JOD\",\"price\":\"JOD0.0008153960\",\"name\":\"Jordanian Dinar\",\"n\":0.0008153960},{\"code\":\"HKD\",\"price\":\"HKD0.00901515475\",\"name\":\"Hong Kong Dollar\",\"n\":0.00901515475},{\"code\":\"RWF\",\"price\":\"RWF1.176450\",\"name\":\"Rwandan Franc\",\"n\":1.176450},{\"code\":\"AED\",\"price\":\"AED0.00422399830\",\"name\":\"United Arab Emirates Dirham\",\"n\":0.00422399830},{\"code\":\"BWP\",\"price\":\"BWP0.01330813005\",\"name\":\"Botswanan Pula\",\"n\":0.01330813005},{\"code\":\"SHP\",\"price\":\"SHP0.00158401460\",\"name\":\"Saint Helena Pound\",\"n\":0.00158401460},{\"code\":\"TRY\",\"price\":\"TRY0.01696146960\",\"name\":\"Turkish Lira\",\"n\":0.01696146960},{\"code\":\"LBP\",\"price\":\"LBP1.74344948105\",\"name\":\"Lebanese Pound\",\"n\":1.74344948105},{\"code\":\"TJS\",\"price\":\"TJS0.01426534520\",\"name\":\"Tajikistani Somoni\",\"n\":0.01426534520},{\"code\":\"IDR\",\"price\":\"IDR16.5262475\",\"name\":\"Indonesian Rupiah\",\"n\":16.5262475},{\"code\":\"KYD\",\"price\":\"KYD0.00095875615\",\"name\":\"Cayman Islands Dollar\",\"n\":0.00095875615},{\"code\":\"AMD\",\"price\":\"AMD0.54704396345\",\"name\":\"Armenian Dram\",\"n\":0.54704396345},{\"code\":\"GHS\",\"price\":\"GHS0.0085679485\",\"name\":\"Ghanaian Cedi\",\"n\":0.0085679485},{\"code\":\"GYD\",\"price\":\"GYD0.24069300475\",\"name\":\"Guyanaese Dollar\",\"n\":0.24069300475},{\"code\":\"KPW\",\"price\":\"KPW1.0350004025\",\"name\":\"North Korean Won\",\"n\":1.0350004025},{\"code\":\"BOB\",\"price\":\"BOB0.00790952635\",\"name\":\"Bolivian Boliviano\",\"n\":0.00790952635},{\"code\":\"KHR\",\"price\":\"KHR4.65750040365\",\"name\":\"Cambodian Riel\",\"n\":4.65750040365},{\"code\":\"MDL\",\"price\":\"MDL0.02114008545\",\"name\":\"Moldovan Leu\",\"n\":0.02114008545},{\"code\":\"AUD\",\"price\":\"AUD0.0015446455\",\"name\":\"Australian Dollar\",\"n\":0.0015446455},{\"code\":\"ILS\",\"price\":\"ILS0.0037055645\",\"name\":\"Israeli New Sheqel\",\"n\":0.0037055645},{\"code\":\"TZS\",\"price\":\"TZS2.67030038640\",\"name\":\"Tanzanian Shilling\",\"n\":2.67030038640},{\"code\":\"VND\",\"price\":\"VND26.291300\",\"name\":\"Vietnamese Dong\",\"n\":26.291300},{\"code\":\"XAU\",\"price\":\"XAU5.9110E-7\",\"name\":\"Gold (troy ounce)\",\"n\":5.9110E-7},{\"code\":\"ZMW\",\"price\":\"ZMW0.02016220020\",\"name\":\"Zambian Kwacha\",\"n\":0.02016220020},{\"code\":\"LRD\",\"price\":\"LRD0.17549434125\",\"name\":\"Liberian Dollar\",\"n\":0.17549434125},{\"code\":\"XAG\",\"price\":\"XAG0.00004640595\",\"name\":\"Silver (troy ounce)\",\"n\":0.00004640595},{\"code\":\"ALL\",\"price\":\"ALL0.12822958735\",\"name\":\"Albanian Lek\",\"n\":0.12822958735},{\"code\":\"CHF\",\"price\":\"CHF0.00107568240\",\"name\":\"Swiss Franc\",\"n\":0.00107568240},{\"code\":\"HRK\",\"price\":\"HRK0.00798123460\",\"name\":\"Croatian Kuna\",\"n\":0.00798123460},{\"code\":\"DJF\",\"price\":\"DJF0.20437845310\",\"name\":\"Djiboutian Franc\",\"n\":0.20437845310},{\"code\":\"XAF\",\"price\":\"XAF0.69323495575\",\"name\":\"CFA Franc BEAC\",\"n\":0.69323495575},{\"code\":\"KGS\",\"price\":\"KGS0.10656417960\",\"name\":\"Kyrgystani Som\",\"n\":0.10656417960},{\"code\":\"SOS\",\"price\":\"SOS0.66642921705\",\"name\":\"Somali Shilling\",\"n\":0.66642921705},{\"code\":\"VEF\",\"price\":\"VEF245904755.6887837775\",\"name\":\"Venezuelan Bolívar Fuerte\",\"n\":245904755.6887837775},{\"code\":\"VUV\",\"price\":\"VUV0.13123409460\",\"name\":\"Vanuatu Vatu\",\"n\":0.13123409460},{\"code\":\"LAK\",\"price\":\"LAK13.65337934930\",\"name\":\"Laotian Kip\",\"n\":13.65337934930},{\"code\":\"BND\",\"price\":\"BND0.00156749025\",\"name\":\"Brunei Dollar\",\"n\":0.00156749025},{\"code\":\"ZMK\",\"price\":\"ZMK10.35138413195\",\"name\":\"Zambian Kwacha (pre-2013)\",\"n\":10.35138413195},{\"code\":\"ETB\",\"price\":\"ETB0.05888445740\",\"name\":\"Ethiopian Birr\",\"n\":0.05888445740},{\"code\":\"JEP\",\"price\":\"JEP0.00088200285\",\"name\":\"Jersey Pound\",\"n\":0.00088200285},{\"code\":\"DZD\",\"price\":\"DZD0.16491086710\",\"name\":\"Algerian Dinar\",\"n\":0.16491086710},{\"code\":\"PAB\",\"price\":\"PAB0.00115046460\",\"name\":\"Panamanian Balboa\",\"n\":0.00115046460},{\"code\":\"GGP\",\"price\":\"GGP0.00088200285\",\"name\":\"Guernsey Pound\",\"n\":0.00088200285},{\"code\":\"SGD\",\"price\":\"SGD0.00156802960\",\"name\":\"Singapore Dollar\",\"n\":0.00156802960},{\"code\":\"MKD\",\"price\":\"MKD0.06517736780\",\"name\":\"Macedonian Denar\",\"n\":0.06517736780},{\"code\":\"USD\",\"price\":\"USD0.001150\",\"name\":\"United States Dollar\",\"n\":0.001150}]");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray;
    }

    public static void updateFeePerKb(Context app) {
        String jsonString = "{\"fee_per_kb\":\"1000000\",\"fee_per_kb_economy\":\"500000\",\"fee_per_kb_luxury\":\"2000000\"}";
        if (jsonString == null || jsonString.isEmpty()) {
            Timber.i("updateFeePerKb: failed to update fee, response string: %s", jsonString);
            return;
        }
        try {
            JSONObject obj = new JSONObject(jsonString);
            // TODO: Refactor when mobile-api v0.4.0 is in prod
            long regularFee = obj.optLong("fee_per_kb");
            long economyFee = obj.optLong("fee_per_kb_economy");
            long luxuryFee = obj.optLong("fee_per_kb_luxury");
            FeeManager.getInstance().setFees(luxuryFee, regularFee, economyFee);
            BRSharedPrefs.putFeeTime(app, System.currentTimeMillis()); //store the time of the last successful fee fetch
        } catch (JSONException e) {
            Timber.e(new IllegalArgumentException("updateFeePerKb: FAILED: " + jsonString, e));
        }
    }

    private static String urlGET(Context app, String myURL) {
        Request request = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get().build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request, false, 0);

        try {
            if (resp == null) {
                Timber.i("urlGET: %s resp is null", myURL);
                return null;
            }
            response = resp.body().string();
            String strDate = resp.header("date");
            if (strDate == null) {
                Timber.i("urlGET: strDate is null!");
                return response;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            Timber.e(e);
        } finally {
            if (resp != null) resp.close();
        }
        return response;
    }
}
