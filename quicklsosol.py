# -*- coding: utf-8 -*-
"""
Created on Tue Mar 23 20:51:54 2021

@author: hossa
"""



from flask import Flask
from flask import request, jsonify
from datetime import timedelta
import pickle
import json

import pandas as pd
import numpy as np
import math
import matplotlib.pyplot as plt
import seaborn as sns
import sys
print(sys.executable)
#from prettytable import PrettyTable
import warnings
warnings.filterwarnings('ignore')
from sklearn.model_selection import train_test_split
from sklearn.model_selection import GridSearchCV
import nltk
nltk.download('stopwords')
from nltk.corpus import stopwords
from tqdm import tqdm
import re
import collections
from wordcloud import STOPWORDS
from scipy.sparse import csr_matrix
from nltk.sentiment.vader import SentimentIntensityAnalyzer
nltk.download('vader_lexicon')
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.preprocessing import StandardScaler
from sklearn.preprocessing import Normalizer
from sklearn.preprocessing import MinMaxScaler
from sklearn.linear_model import Ridge
from sklearn.linear_model import Lasso
from sklearn.linear_model import SGDRegressor
from lightgbm import LGBMRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error
from sklearn.metrics import mean_squared_log_error
from wordcloud import WordCloud
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import Lasso
from scipy.sparse import hstack

app = Flask(__name__)



@app.route('/price', methods=['POST'])
def main():
   
    #train_data = pd.read_table('train.tsv',sep='\t')
    #train_data=train_data.head(5000)
    #print(train_data.shape)
    #print(train_data.isnull().sum())
    
    
  
 
    content= request.data
    print (content)
    content=json.loads(content.decode('utf-8'))
    name=content["name"]
    print("name:",name )
    #name=request.get_json()['name']
    con=content["con"]
    cat=content["cat"]
    brand=content["brand"]
    #price=content["price"]
    shipping=content["shipping"]
    desc=content["desc"]
    #user_id=None
    
    cars = {'name': [name],
            'item_condition_id': [con],
            'category_name': [cat],
            'brand_name': [brand],
           # 'price': [price],
            'shipping': [shipping],
            'item_description': [desc]
            }




    df = pd.DataFrame(cars, columns = ['name','item_condition_id','category_name','brand_name','shipping','item_description'])

    print (df)
  
    from nltk.corpus import stopwords
    def handle_category(data):
        #"""this function splits the category_name into further three sub_categories."""
        cat1=[]
        cat2=[]
        cat3=[]
        i=0
        for row in data:
          try:
            categories=row.split('/')
          except:
            categories=['','','']
          cat1.append(categories[0])
          cat2.append(categories[1])
          cat3.append(categories[2])
          i+=1
        return cat1,cat2,cat3



    test_data=df
    
  
    test=test_data.copy()
       
    print("shape of the test data: ",test_data.shape)
    test_data.isnull().sum()
    
    print("Number of Nan values in category_name: {}%".format((test_data['category_name'].isnull().sum()/test_data.shape[0])*100))
    print("Number of Nan values in brand_name: {}%".format((test_data['brand_name'].isnull().sum()/test_data.shape[0])*100))
    print("Number of Nan values in item description: {}%".format((test_data['item_description'].isnull().sum()/test_data.shape[0])*100))
    
    
    c1,c2,c3=handle_category(test_data['category_name'])
    test_data['sub_category1']=c1
    test_data['sub_category2']=c2
    test_data['sub_category3']=c3
    
    
    test_data['brand_name'].fillna(value='Not known',inplace=True)
    test_data['item_description'].fillna(value='No description given',inplace=True)
    test_data.isnull().sum()
    
    
    
    stopwords=set(stopwords.words('english'))
    
    
    
    def stopwords_count(data):
      """this function counts the number of stopwords in each of the item_description"""
      count_stopwords=[]
      for i in tqdm(data['item_description']):
        count=0
        for j in i.split(' '):
          if j in stopwords: count+=1  #finding if the word is present in the nltk stopwords or not
        count_stopwords.append(count)
      return count_stopwords
    
    
    
    test_data['count_stopwords']=stopwords_count(test_data)
    
    
    
    # https://stackoverflow.com/a/47091490/4084039
    def decontracted(phrase):
        """this function removies shorthands for the textual data..."""
        phrase = re.sub(r"won't", "will not", phrase)
        phrase = re.sub(r"can\'t", "can not", phrase)
        phrase = re.sub(r"n\'t", " not", phrase)
        phrase = re.sub(r"\'re", " are", phrase)
        phrase = re.sub(r"\'s", " is", phrase)
        phrase = re.sub(r"\'d", " would", phrase)
        phrase = re.sub(r"\'ll", " will", phrase)
        phrase = re.sub(r"\'t", " not", phrase)
        phrase = re.sub(r"\'ve", " have", phrase)
        phrase = re.sub(r"\'m", " am", phrase)
        return phrase
    
        
        
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
        
        
       
        if (("good" or "great" ) in desstr):
            #corr=1
            per=per+0.2
      #      per=per+0.1
            
        if (("new") in desstr):
            #corr=1
            per=per+0.3
       #     per=per+0.3
        
        
        if (("broken" or "damaged" or "crack") in desstr):
            #corr=1
            per=per-0.9
            
        if (("needs") in desstr):
            #corr=1
            per=per-0.9
        
        if (("except") in desstr):
            #corr=1
            per=per-0.5
                
        
        return(corr,per)
        

    
    
    # https://gist.github.com/sebleier/554280
    def text_preprocessing(data):
      """this function performs preprocessing the item_description """
      preprocessed_total = []
      for sentance in tqdm(data['item_description'].values):
        sent = decontracted(sentance)
        sent = sent.replace('\\r', ' ')
        sent = sent.replace('\\"', ' ')
        sent = sent.replace('\\n', ' ')
        sent = re.sub('[^A-Za-z0-9]+', ' ', sent)
        sent = ' '.join(e for e in sent.split() if e.lower() not in stopwords)  #removing stop words
        preprocessed_total.append(sent.lower().strip())
      return preprocessed_total
    
    
    test_data['item_description']=text_preprocessing(test_data)
    
    stopwords=set(STOPWORDS)
    
    
    
    
    
    def description_length(data):
      """this function finds the length of the description basing on spaces in the statement"""
      description_length=[]
      for i in data['item_description']:
        description_length.append(len(i.split(' '))) #splitting statement using spaces and finding length of it
      return description_length
    
    
    
    
    print("processing item_description in test_data...")
    
    test_data['description_length']=description_length(test_data)
    #print(test_data.iloc[100]['item_description'],test_data.iloc[100]['description_length'])
    
    
    
    
    
    
    def branded(data):
       """this function assigns a value 1 if a product has brand_name else 0"""
       is_branded=[]
       for i in data['brand_name']:
          if i=='Not known': is_branded.append(0) #if it is a Nan value i.e.. unknown brand make it as 0.
          else: is_branded.append(1)
       return is_branded
    
    test_data['is_branded']=branded(test_data)
    
    
    def sentiment_analysis(data):
       """this function performs sentiment score analysis of each datapoint"""
       sentiment_score = SentimentIntensityAnalyzer()
       sentiment = []
       for sentence in tqdm(data):
           sentiment.append(sentiment_score.polarity_scores(sentence))
       return sentiment
    
    
    
    testing_sentiment_score=sentiment_analysis(test_data['item_description'])
    
    def splitting_sentiment(sentiment_score):
      """this function splits sentiment analysis score into four further features ie positive,negative,compound and neutral"""
      positive=[]
      negative=[]
      neutral=[]
      compound=[]
      for i in sentiment_score:
        positive.append(i['pos'])
        negative.append(i['neg'])
        neutral.append(i['neu'])
        compound.append(i['compound'])
      return positive,negative,neutral,compound
    
    
    
    
    
    
    print("Testing Data Sentiment Analysis: ")
    pos,neg,neu,comp=splitting_sentiment(testing_sentiment_score)
    test_data['positive']=pos
    test_data['negative']=neg
    test_data['neutral']=neu
    test_data['compound']=comp
    #print(test_data.iloc[50]['item_description'])
    print(testing_sentiment_score[0])
    
    
    
    
    
    with open('countv1','rb') as f:
        countv1=pickle.load(f)
    
                    #fitting
    
    bow_cat1_test=countv1.transform(test_data['sub_category1'])
    
    
    with open('countv2','rb') as f:
        countv2=pickle.load(f)
    
    bow_cat2_test=countv2.transform(test_data['sub_category2'])
    
    
    with open('countv22','rb') as f:
        countv22=pickle.load(f)
    
    
    bow_cat3_test=countv22.transform(test_data['sub_category3'])
    
    
    with open('countv3','rb') as f:
        countv3=pickle.load(f)
    
    
    bow_brand_test=countv3.transform(test_data['brand_name'])
    
    
    
    with open('countv5','rb') as f:
        countv5=pickle.load(f)
    
    bow_name_test=countv5.transform(test_data['name'])
    
    
    
    
    with open('tf1','rb') as f:
        tf1=pickle.load(f)
    
    tfidf_description_test=tf1.transform(test_data['item_description'])
    #print("After Vectorization of item description feature: ")
    #print(tfidf_description_train.shape)
    #print(tfidf_description_cv.shape)
    #print(tfidf_description_test.shape)
    #print("Some Features are: ")
    #print(tfidfvectorizer.get_feature_names()[3025:3050])  #getting 25 random features.
    
    
    
    #scaler=StandardScaler().fit(np.array(train_data['positive']).reshape(-1,1))   #fitting
    
    #with open('s1', 'wb') as fout:
    #    pickle.dump(scaler, fout)
    
    with open('s1','rb') as f:
        s1=pickle.load(f)
    
    positive_test = s1.transform(np.array(test_data['positive']).reshape(-1,1))
    #print(positive_train[50:55].reshape(1,-1)[0])    #printing 5 random postive sentiment scores 
    
    
    
    
    with open('scalers2to6','rb') as f:
        s2,s3,s4,s5,s6=pickle.load(f)
    
    
    negative_test=s2.transform(np.array(test_data['negative']).reshape(-1,1))
    #print(negative_train[25:30].reshape(1,-1)[0])    #printing 5 random negative sentiment score
    #print("After Preprocessing of negative sentiment score:")
    #print(negative_train.shape)
    #print(negative_cv.shape)
    #print(negative_test.shape)
    #print("="*125)
    
    neutral_test=s3.transform(np.array(test_data['neutral']).reshape(-1,1))
    #print(neutral_train[5:10].reshape(1,-1)[0])     #printing 5 random neutral sentiment score
    #print("After Preprocessing of neutral sentiment score:")
    #print(neutral_train.shape)
    #print(neutral_cv.shape)
    #print(neutral_test.shape)
    #print("="*125)
    
    compound_test=s4.transform(np.array(test_data['compound']).reshape(-1,1))
    #print(compound_train[35:40].reshape(1,-1)[0])   #printing 5 random compound sentiment score
    #print("After Preprocessing of compound sentiment score:")
    #print(compound_train.shape)
    #print(compound_cv.shape)
    #print(compound_test.shape)
    #print("="*125)
    
    length_test=s5.transform(np.array(test_data['description_length']).reshape(-1,1))
    #print(length_train[1:5].reshape(1,-1)[0])       #printing 5 random description lengths
    #print("After Preprocessing of description length:")
    #print(length_train.shape)
    #print(length_cv.shape)
    #print(length_test.shape)
    #print("="*125)
    
    stopword_test=s6.transform(np.array(test_data['count_stopwords']).reshape(-1,1))
    #print(stopword_train[15:20].reshape(1,-1)[0])   #printing 5 random stopwords count
    #print("After Preprocessing of count_stopwords feature:")
    #print(stopword_train.shape)
    #print(stopword_cv.shape)
    #print(stopword_test.shape)
    
    
    
    
    
    
    
    features_test = csr_matrix(pd.get_dummies(test_data[['item_condition_id', 'shipping','is_branded']],sparse=True).values)
    #print("shape",features_train.shape)
    #print(features_train)
    
    print(features_test.shape)
    
    
    
    
    #https://stackoverflow.com/questions/43018711/about-numpys-concatenate-hstack-vstack-functions
    
    #X_train=hstack((bow_cat1_train,bow_cat2_train,bow_cat3_train,bow_brand_train,bow_name_train,tfidf_description_train,positive_train,negative_train,neutral_train,compound_train,features_train,length_train,stopword_train)).tocsr()
    
    X_test=hstack((bow_cat1_test,bow_cat2_test,bow_cat3_test,bow_brand_test,bow_name_test,tfidf_description_test,positive_test,negative_test,neutral_test,compound_test,features_test,length_test,stopword_test)).tocsr()
    #print("Shape of train data: ",X_train.shape) #train
    
    print("Shape of test data: ",X_test.shape)   #test
    
    
    
    lasso = Lasso(alpha=0.00001,fit_intercept=False)
    print("Model is fitting!!!")
    #lasso.fit(X_train, train_data['target'])
    
    #with open('model_pickle','wb') as f:
    #    pickle.dump(lasso,f)
    
    
    with open('model_pickle','rb') as f:
        mod=pickle.load(f)
        
    print("imp pred")
    
    print("done")
    
    
    
    
    ytesting=mod.predict(X_test[0])
    
    #yteststr=str(ytesting)
    #print(yteststr)
    
    print(df.head(1))
    
    corr=0
    per=1
    (corr,per)=makecor(df,corr,per)
    


    res = np.exp(ytesting[0])-1
    res = np.round(res, 1)
    finalres=per*res+corr
    finalres = np.round(finalres, 1)
    finalres=str(finalres)
    print(finalres)
    return jsonify({'greetings': finalres}) 
    
    
    
    
    
    
    
if __name__=="__main__":
    app.run(debug=True)

    
    
    
    
    
    
    
    
    
    
