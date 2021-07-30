# -*- coding: utf-8 -*-
"""
Created on Wed Apr 21 12:37:52 2021

@author: hossa
"""
#i just print

  
def makecor(df,corr,per):
    catstr=df['category_name'][0]
    b=(df['brand_name'][0]).lower()
    con=int(df['item_condition_id'][0])
    sh=int(df['shipping'][0])
    namestr=(df['name'][0]).lower()
    desstr=(df['item_description'][0]).lower()
        
    if sh==1:
        #per=per+0.1
        corr=corr+5
    if con==5:
        per=per-0.9
    elif con==4:
        per=per-0.7
    elif con==3:
        if (" & Smartphones" in catstr):
            corr=100
            per=per+2
        per=per-0.5
    elif con==2:
        if (" & Smartphones" in catstr):
            corr=100
            per=per+2 
        else:
            per=per-0.3
    elif con==1:
         if (" & Smartphones" in catstr):
            corr=300
            per=per+3    
         else:
            per=per+0.5
        
    if (len(desstr)>200):
        per=per-0.8
      
    if (b=="apple"):
        per=per+1
    
    if (" & Smartphones" in catstr):
        #corr=100
        per=per+0.5
        
    if ("Shoes" in catstr):
        #corr=100
        per=per-0.4
    
    elif ("/Headphones" in catstr):
      #  corr=50
        per=per+1.5
        
    #print(int('iphone12')) 
    if ("new" in namestr):
        #corr=100
        per=per+0.2
    
    if ("iphone x" in namestr):
        corr=corr+100
        #per=per+0.5
        
    if ("7 128gb"in namestr):
        corr=corr-300
        #per=per+0.5
        
     
    for s in namestr.split():
        if s.isdigit():
            #corr=corr+int(s)*10
            per=per+int(s)/10
            print(per)
    
    
    if "couch" in namestr:
        corr=corr+200
    elif "tv" in namestr:
        corr=corr+150
        if (con>2):
            corr=corr-50
    elif "cup" in namestr:
        corr=corr-4
   
    if (("good" or "great" ) in desstr):
        #corr=1
        per=per+0.2
  #      per=per+0.1
        
    if (("new") in desstr):
        #corr=1
        per=per+0.3
   #     per=per+0.3
    
    
    if (("broken" or "damaged" or "crack" or "scratch") in desstr):
        #corr=1
        per=per-0.9
        
    if (("needs") in desstr):
        #corr=1
        per=per-0.9
    
    if (("except") in desstr):
        #corr=1
        per=per-0.5
            
    
    return(corr,per)
    
