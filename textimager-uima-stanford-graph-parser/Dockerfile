FROM python:2.7

WORKDIR /usr/src/app

COPY requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

COPY src/main/python ./

RUN ls

EXPOSE 81

ENV FLASK_APP parserRest.py

CMD [ "python", "-m", "flask", "run", "-p", "81", "--host", "0.0.0.0" ]
