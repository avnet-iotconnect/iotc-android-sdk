package com.iotconnectsdk.webservices.responsebean;

import java.util.List;

public class SyncServiceResponse {

    /**
     * d : {"sc":{"hb":{"fq":10,"h":"","un":"","pwd":"","pub":""},"log":{"h":"","un":"","pwd":"","pub":""},"sf":0},"p":{"n":"mqtt","h":"demohub.azure-devices.net","p":8883,"id":"HPL-MACANDROID","un":"demohub.azure-devices.net/HPL-MACANDROID","pwd":"SharedAccessSignature sr=demohub.azure-devices.net%2Fdevices%2FHPL-MACANDROID&sig=ANUe0EGixpZ%2B0PYpvU%2FfUzIgWhBlXYIPepgl%2BC65Ov8%3D&se=1551787917&skn=iothubowner","pub":"devices/HPL-MACANDROID/messages/events/","sub":"devices/HPL-MACANDROID/messages/devicebound/#"},"d":[{"tg":"","id":"MACANDROID","s":0}],"att":[{"p":"","dt":null,"agt":0,"tw":"","tg":"","d":[{"ln":"temp1","dt":0,"dv":"1to15","tg":"","sq":1,"agt":0,"tw":""},{"ln":"humidity","dt":0,"dv":"","tg":"","sq":2,"agt":0,"tw":""},{"ln":"temp","dt":0,"dv":"","tg":"","sq":4,"agt":0,"tw":""}]},{"p":"gyro","dt":2,"agt":0,"tw":"","tg":"","d":[{"ln":"x","dt":0,"dv":"-3to3","tg":"","sq":1,"agt":0,"tw":""},{"ln":"y","dt":0,"dv":"","tg":"","sq":2,"agt":0,"tw":""}]}],"set":null,"r":null,"dtg":"ad4a5b15-35f0-4ab9-815e-c0b584bc9ea4","cpId":"HPL","rc":0,"ee":0,"at":1}
     */

    private DBeanXX d;

    public DBeanXX getD() {
        return d;
    }

    public void setD(DBeanXX d) {
        this.d = d;
    }

    public static class DBeanXX {
        /**
         * sc : {"hb":{"fq":10,"h":"","un":"","pwd":"","pub":""},"log":{"h":"","un":"","pwd":"","pub":""},"sf":0}
         * p : {"n":"mqtt","h":"demohub.azure-devices.net","p":8883,"id":"HPL-MACANDROID","un":"demohub.azure-devices.net/HPL-MACANDROID","pwd":"SharedAccessSignature sr=demohub.azure-devices.net%2Fdevices%2FHPL-MACANDROID&sig=ANUe0EGixpZ%2B0PYpvU%2FfUzIgWhBlXYIPepgl%2BC65Ov8%3D&se=1551787917&skn=iothubowner","pub":"devices/HPL-MACANDROID/messages/events/","sub":"devices/HPL-MACANDROID/messages/devicebound/#"}
         * d : [{"tg":"","id":"MACANDROID","s":0}]
         * att : [{"p":"","dt":null,"agt":0,"tw":"","tg":"","d":[{"ln":"temp1","dt":0,"dv":"1to15","tg":"","sq":1,"agt":0,"tw":""},{"ln":"humidity","dt":0,"dv":"","tg":"","sq":2,"agt":0,"tw":""},{"ln":"temp","dt":0,"dv":"","tg":"","sq":4,"agt":0,"tw":""}]},{"p":"gyro","dt":2,"agt":0,"tw":"","tg":"","d":[{"ln":"x","dt":0,"dv":"-3to3","tg":"","sq":1,"agt":0,"tw":""},{"ln":"y","dt":0,"dv":"","tg":"","sq":2,"agt":0,"tw":""}]}]
         * set : null
         * r : null
         * dtg : ad4a5b15-35f0-4ab9-815e-c0b584bc9ea4
         * cpId : HPL
         * rc : 0
         * ee : 0
         * at : 1
         */

        private ScBean sc;
        private PBean p;
        private Object set;
        private List<RuleBean> r;
        private String dtg;
        private String cpId;
        private int rc;
        private int ee;
        private int at;
        private List<DBean> d;
        private List<AttBean> att;

        public ScBean getSc() {
            return sc;
        }

        public void setSc(ScBean sc) {
            this.sc = sc;
        }

        public PBean getP() {
            return p;
        }

        public void setP(PBean p) {
            this.p = p;
        }

        public Object getSet() {
            return set;
        }

        public void setSet(Object set) {
            this.set = set;
        }

//        public Object getR() {
//            return r;
//        }

//        public void setR(Object r) {
//            this.r = r;
//        }


        public List<RuleBean> getR() {
            return r;
        }

        public void setR(List<RuleBean> r) {
            this.r = r;
        }

        public String getDtg() {
            return dtg;
        }

        public void setDtg(String dtg) {
            this.dtg = dtg;
        }

        public String getCpId() {
            return cpId;
        }

        public void setCpId(String cpId) {
            this.cpId = cpId;
        }

        public int getRc() {
            return rc;
        }

        public void setRc(int rc) {
            this.rc = rc;
        }

        public int getEe() {
            return ee;
        }

        public void setEe(int ee) {
            this.ee = ee;
        }

        public int getAt() {
            return at;
        }

        public void setAt(int at) {
            this.at = at;
        }

        public List<DBean> getD() {
            return d;
        }

        public void setD(List<DBean> d) {
            this.d = d;
        }

        public List<AttBean> getAtt() {
            return att;
        }

        public void setAtt(List<AttBean> att) {
            this.att = att;
        }

        public class RuleBean {
            private String g;
            private String es;
            private String con;
            private List<RuleAtt> att = null;
            private String cmd;

            public String getG() {
                return g;
            }

            public void setG(String g) {
                this.g = g;
            }

            public String getEs() {
                return es;
            }

            public void setEs(String es) {
                this.es = es;
            }

            public String getCon() {
                return con;
            }

            public void setCon(String con) {
                this.con = con;
            }

            public List<RuleAtt> getAtt() {
                return att;
            }

            public void setAtt(List<RuleAtt> att) {
                this.att = att;
            }

            public String getCmd() {
                return cmd;
            }

            public void setCmd(String cmd) {
                this.cmd = cmd;
            }
        }

        public class RuleAtt {
            private List<String> g = null;

            public List<String> getG() {
                return g;
            }

            public void setG(List<String> g) {
                this.g = g;
            }
        }

        public static class ScBean {
            /**
             * hb : {"fq":10,"h":"","un":"","pwd":"","pub":""}
             * log : {"h":"","un":"","pwd":"","pub":""}
             * sf : 0
             * df : 0
             */

            private HbBean hb;
            private LogBean log;
            private int sf;
            private int df;

            public HbBean getHb() {
                return hb;
            }

            public void setHb(HbBean hb) {
                this.hb = hb;
            }

            public LogBean getLog() {
                return log;
            }

            public void setLog(LogBean log) {
                this.log = log;
            }

            public int getSf() {
                return sf;
            }

            public void setSf(int sf) {
                this.sf = sf;
            }

            public int getDf() {
                return df;
            }

            public void setDf(int df) {
                this.df = df;
            }

            public static class HbBean {
                /**
                 * fq : 10
                 * h :
                 * un :
                 * pwd :
                 * pub :
                 */

                private int fq;
                private String h;
                private String un;
                private String pwd;
                private String pub;

                public int getFq() {
                    return fq;
                }

                public void setFq(int fq) {
                    this.fq = fq;
                }

                public String getH() {
                    return h;
                }

                public void setH(String h) {
                    this.h = h;
                }

                public String getUn() {
                    return un;
                }

                public void setUn(String un) {
                    this.un = un;
                }

                public String getPwd() {
                    return pwd;
                }

                public void setPwd(String pwd) {
                    this.pwd = pwd;
                }

                public String getPub() {
                    return pub;
                }

                public void setPub(String pub) {
                    this.pub = pub;
                }
            }

            public static class LogBean {
                /**
                 * h :
                 * un :
                 * pwd :
                 * pub :
                 */

                private String h;
                private String un;
                private String pwd;
                private String pub;

                public String getH() {
                    return h;
                }

                public void setH(String h) {
                    this.h = h;
                }

                public String getUn() {
                    return un;
                }

                public void setUn(String un) {
                    this.un = un;
                }

                public String getPwd() {
                    return pwd;
                }

                public void setPwd(String pwd) {
                    this.pwd = pwd;
                }

                public String getPub() {
                    return pub;
                }

                public void setPub(String pub) {
                    this.pub = pub;
                }
            }
        }

        public static class PBean {
            /**
             * n : mqtt
             * h : demohub.azure-devices.net
             * p : 8883
             * id : HPL-MACANDROID
             * un : demohub.azure-devices.net/HPL-MACANDROID
             * pwd : SharedAccessSignature sr=demohub.azure-devices.net%2Fdevices%2FHPL-MACANDROID&sig=ANUe0EGixpZ%2B0PYpvU%2FfUzIgWhBlXYIPepgl%2BC65Ov8%3D&se=1551787917&skn=iothubowner
             * pub : devices/HPL-MACANDROID/messages/events/
             * sub : devices/HPL-MACANDROID/messages/devicebound/#
             */

            private String n;
            private String h;
            private int p;
            private String id;
            private String un;
            private String pwd;
            private String pub;
            private String sub;

            public String getN() {
                return n;
            }

            public void setN(String n) {
                this.n = n;
            }

            public String getH() {
                return h;
            }

            public void setH(String h) {
                this.h = h;
            }

            public int getP() {
                return p;
            }

            public void setP(int p) {
                this.p = p;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getUn() {
                return un;
            }

            public void setUn(String un) {
                this.un = un;
            }

            public String getPwd() {
                return pwd;
            }

            public void setPwd(String pwd) {
                this.pwd = pwd;
            }

            public String getPub() {
                return pub;
            }

            public void setPub(String pub) {
                this.pub = pub;
            }

            public String getSub() {
                return sub;
            }

            public void setSub(String sub) {
                this.sub = sub;
            }
        }

        public static class DBean {
            /**
             * tg :
             * id : MACANDROID
             * s : 0
             */

            private String tg;
            private String id;
            private int s;

            public String getTg() {
                return tg;
            }

            public void setTg(String tg) {
                this.tg = tg;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public int getS() {
                return s;
            }

            public void setS(int s) {
                this.s = s;
            }
        }

        public static class AttBean {
            /**
             * p :
             * dt : null
             * agt : 0
             * tw :
             * tg :
             * d : [{"ln":"temp1","dt":0,"dv":"1to15","tg":"","sq":1,"agt":0,"tw":""},{"ln":"humidity","dt":0,"dv":"","tg":"","sq":2,"agt":0,"tw":""},{"ln":"temp","dt":0,"dv":"","tg":"","sq":4,"agt":0,"tw":""}]
             */

            private String p;
            private Object dt;
            private int agt;
            private String tw;
            private String tg;
            private List<DBeanX> d;

            public String getP() {
                return p;
            }

            public void setP(String p) {
                this.p = p;
            }

            public Object getDt() {
                return dt;
            }

            public void setDt(Object dt) {
                this.dt = dt;
            }

            public int getAgt() {
                return agt;
            }

            public void setAgt(int agt) {
                this.agt = agt;
            }

            public String getTw() {
                return tw;
            }

            public void setTw(String tw) {
                this.tw = tw;
            }

            public String getTg() {
                return tg;
            }

            public void setTg(String tg) {
                this.tg = tg;
            }

            public List<DBeanX> getD() {
                return d;
            }

            public void setD(List<DBeanX> d) {
                this.d = d;
            }

            public static class DBeanX {
                /**
                 * ln : temp1
                 * dt : 0
                 * dv : 1to15
                 * tg :
                 * sq : 1
                 * agt : 0
                 * tw :
                 */

                private String ln;
                private int dt;
                private String dv;
                private String tg;
                private int sq;
                private int agt;
                private String tw;

                public String getLn() {
                    return ln;
                }

                public void setLn(String ln) {
                    this.ln = ln;
                }

                public int getDt() {
                    return dt;
                }

                public void setDt(int dt) {
                    this.dt = dt;
                }

                public String getDv() {
                    return dv;
                }

                public void setDv(String dv) {
                    this.dv = dv;
                }

                public String getTg() {
                    return tg;
                }

                public void setTg(String tg) {
                    this.tg = tg;
                }

                public int getSq() {
                    return sq;
                }

                public void setSq(int sq) {
                    this.sq = sq;
                }

                public int getAgt() {
                    return agt;
                }

                public void setAgt(int agt) {
                    this.agt = agt;
                }

                public String getTw() {
                    return tw;
                }

                public void setTw(String tw) {
                    this.tw = tw;
                }
            }
        }
    }
}
