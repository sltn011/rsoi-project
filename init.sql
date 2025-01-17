CREATE TABLE public.cars
(
    id                  SERIAL PRIMARY KEY,
    car_uid             uuid UNIQUE NOT NULL,
    brand               VARCHAR(80) NOT NULL,
    model               VARCHAR(80) NOT NULL,
    registration_number VARCHAR(20) NOT NULL,
    power               INT,
    price               INT         NOT NULL,
    type                VARCHAR(20)
        CHECK (type IN ('SEDAN', 'SUV', 'MINIVAN', 'ROADSTER')),
    availability        BOOLEAN     NOT NULL
);

CREATE TABLE public.rental
(
    id          SERIAL PRIMARY KEY,
    rental_uid  uuid UNIQUE              NOT NULL,
    username    VARCHAR(80)              NOT NULL,
    payment_uid uuid                     NOT NULL,
    car_uid     uuid                     NOT NULL,
    date_from   TIMESTAMP WITH TIME ZONE NOT NULL,
    date_to     TIMESTAMP WITH TIME ZONE NOT NULL,
    status      VARCHAR(20)              NOT NULL
        CHECK (status IN ('IN_PROGRESS', 'FINISHED', 'CANCELED'))
);

CREATE TABLE payment
(
    id          SERIAL PRIMARY KEY,
    payment_uid uuid        NOT NULL,
    status      VARCHAR(20) NOT NULL
        CHECK (status IN ('PAID', 'CANCELED')),
    price       INT         NOT NULL
);

CREATE TABLE public.accounts
(
    id          SERIAL PRIMARY KEY,
    accUid      uuid UNIQUE              NOT NULL,
    username    VARCHAR(20)              NOT NULL,
    email       VARCHAR(40)              NOT NULL,
    password    VARCHAR(20)              NOT NULL,
    role        VARCHAR(8)               NOT NULL,
        CHECK (role IN ('USER', 'ADMIN'))
);

INSERT INTO public.cars(
	id, car_uid, brand, model, registration_number, power, price, type, availability)
	VALUES (0, '109b42f3-198d-4c89-9276-a7520a7120ab', 'Mercedes Benz', 'GLA 250', 'ЛО777Х799', 249, 3500, 'SEDAN', true);

INSERT INTO public.accounts(
	id, accUid, username, email, password, role)
	VALUES (0, '9d10c23a-61ee-487d-8ed7-dd360298bc94', 'admin', 'admin@rsoi.ru', 'admin', 'ADMIN');
